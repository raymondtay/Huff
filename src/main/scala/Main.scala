package huff.cluster

/** 
 Huff -

  The cluster launcher that loads the `Http` code
  to serve client requests.
 */

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.cluster.{Cluster, ClusterEvent}
import ClusterEvent._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging._

import com.orbitz.consul.option.{ConsistencyMode, ImmutableQueryOptions}
import cats._
import cats.data._
import cats.implicits._

import scala.collection.JavaConverters._

import akka.event.{LoggingAdapter, Logging}

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scala.language.postfixOps

object Huff extends App {
  import huff.http._
  import huff.http.json._
  import huff.config._

  // Get the default logger
  val logger = Logger(Huff.getClass)
  val systemEnvConfig = Config(sys.env)

  import Validator._
  import scala.concurrent.duration._

  //
  // The cluster node is only started when the configuration is 
  // valid and correct; otherwise, the logs would capture
  //
  for {
    c <- getHuffConfig(systemEnvConfig)
  } {
    val data = HuffLog(
      service_name = "Huff Cluster",
      category     = "application",
      event_type   = "operation",
      mesg         = s"Starting up Http service: ${c.listeningPort}"
      )
    logger.info(data.asJson.noSpaces)
    val config = ConfigFactory.load().
      withValue("akka.remote.netty.tcp.bind-hostname",
            ConfigValueFactory.fromAnyRef(ContainerHostIp.load() getOrElse "127.0.0.1")).
      withValue("akka.remote.netty.tcp.hostname",
            ConfigValueFactory.fromAnyRef(ContainerHostIp.load() getOrElse "127.0.0.1")).resolve()
      
    implicit val actorSystem = ActorSystem(c.clusterName, config)

    implicit val actorMaterializer = ActorMaterializer()
    actorSystem.actorOf(Props(classOf[HuffListener], config), name = "HuffListener")

    for {
      heartBeat <- getHuffHeartbeatConfig(Config(ScalaCfg.heartBeatCfg(config)))
    } {
      actorSystem.actorOf(Props(classOf[Heartbeat], heartBeat.initialDelay, heartBeat.interval, heartBeat.message))
    } 

    val server = new RestServer()
    server.startServer(c.hostname, c.listeningPort)
  }
}

/** 
 Heartbeat -
  This actor would schedule a heartbeat message
  with a frequency
*/
class Heartbeat(
  val initialDelay : scala.concurrent.duration.FiniteDuration,
  val interval     : scala.concurrent.duration.FiniteDuration, 
  val heartBeatMsg : String) extends Actor with ActorLogging {

  import huff.http.json.HuffLog
  import scala.concurrent.duration._
  import context.dispatcher
  context.system.scheduler.schedule(initialDelay, interval, self, "Tick")
  val data = HuffLog(
      service_name = "Huff Cluster",
      category     = "application",
      event_type   = "heartbeat",
      mesg         = heartBeatMsg
      )
 
  def receive = {
    case "Tick" ⇒ 
      log.info(data.asJson.noSpaces)
  }
}
/** 
 HuffListener - 

   Cluster event listener which logs certain 
   events we are interested in.
   
   Note: Listener actor is using the same `dispatcher`
         as the implied/given actor system.
         Designate several `dispatcher`(s) as necessary to
         run your code which might potentially block
         e.g. running database / file IO actions

 */
class HuffListener(config: com.typesafe.config.Config) extends Actor with ActorLogging {
  import scala.collection.JavaConversions._
  import scala.concurrent._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  val cluster = Cluster(context.system)

  override def preStart() = {
    cluster.subscribe(self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember])
  }
  override def postStop() = cluster.unsubscribe(self)
  def receive = {
    case MemberUp(member) ⇒ 
      log.info(s"event_type : cluster, msg : 'member @${member.address} is UP'")
    case UnreachableMember(member) ⇒
      log.info(s"event_type : cluster, msg : 'member @'${member.address} is NOT-REACHABLE''")
    case MemberRemoved(member, previousStatus) ⇒
      log.info(s"event_type : cluster, msg : 'member @'${member.address} is REMOVED''")
    case MemberJoined(member) ⇒ 
      log.info(s"member joined : $member")
    case _ : MemberEvent ⇒  // ignore
  }

  val (consulCfg, consulApi) = ConsulApi(config).toOption.head

  val consulEnabled = consulCfg.enabled 

  // This callback is triggered iff all members of the cluster
  // are up.
  cluster.registerOnMemberUp {
    log.info("Cluster is ready!")
    huffScheduler.cancel()
  }

  def logError = (e: Exception) ⇒ log.error(s"Cannot resolve the IP address, detailed message : {${e.getMessage}}")

  // 
  // This is the action that invokes the call to the Consul.io interface
  // and retrieves the healthy nodes and commands them to join the 
  // cluster.
  // 
  val huffScheduler : Cancellable =
    context.system.scheduler.schedule(10 seconds, 30 seconds, new Runnable {
      override def run() : Unit = {

        val selfAddress = cluster.selfAddress

        log.debug(s"Bootstrapping, self address : $selfAddress")
        def getServiceAddresses(implicit actorSystem: ActorSystem = context.system): List[akka.actor.Address] = {
          val queryOpts = ImmutableQueryOptions.builder().consistencyMode(ConsistencyMode.CONSISTENT).build()
          val serviceNodes =
	    consulApi.healthClient().
	    getHealthyServiceInstances(consulCfg.serviceName, queryOpts)

          serviceNodes.getResponse.toList.map{ node ⇒
	    ContainerHostIp.getIpByHostname(node.getService.getAddress).
	      bimap(logError,ConsulApi.constructActorAddress(node, actorSystem.name)).toList
          }.flatten.flatten
        }

        val serviceSeeds = if (consulEnabled) {
	  val serviceAddresses = getServiceAddresses

	  serviceAddresses filter { address ⇒ 
	    address != selfAddress || address == serviceAddresses.head
	  }
	} else List(selfAddress)
        cluster.joinSeedNodes(serviceSeeds)
      }
    })
}
