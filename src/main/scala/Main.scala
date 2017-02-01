package deeplabs.cluster

/** 
 Huff -

  The cluster launcher that loads the `Http` code
  to serve client requests.
 */

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.cluster.{Cluster, ClusterEvent}
import ClusterEvent._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging._

import cats._
import cats.data._
import cats.implicits._

import scala.collection.JavaConverters._

import akka.event.{LoggingAdapter, Logging}

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scala.language.postfixOps

object Huff extends App {
  import deeplabs.http._
  import deeplabs.http.json._
  import deeplabs.config._

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
    val data = DLLog(
      service_name = "Huff cluster",
      category     = "application",
      event_type   = "operation",
      message      = s"Starting up Http service: ${c.listeningPort}"
      )
    logger.info(data.asJson.noSpaces)
    val config = 
        ConfigFactory.load().
        withValue("akka.cluster.seed-nodes",
	  ConfigFactory.parseString(c.seedNodes.mkString("\n")) resolve() getList("akka.cluster.seed-nodes")).
        withValue("akka.remote.netty.tcp.hostname",
	  ConfigValueFactory.fromAnyRef(ContainerHostIp.load() getOrElse "127.0.0.1")).
        withValue("akka.remote.netty.tcp.port",
	  ConfigValueFactory.fromAnyRef(2551)).resolve()
      
    implicit val actorSystem = 
      ActorSystem(c.clusterName, config)

    implicit val actorMaterializer = ActorMaterializer()
    actorSystem.actorOf(Props[HuffListener], name = "HuffListener")

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

  import deeplabs.http.json.DLLog
  import scala.concurrent.duration._
  import context.dispatcher
  context.system.scheduler.schedule(initialDelay, interval, self, "Tick")
  val data = DLLog(
      service_name = "Huff Http Cluster",
      category     = "application",
      event_type   = "heartbeat",
      message      = heartBeatMsg
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
class HuffListener extends Actor with ActorLogging {
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
    case _ : MemberEvent ⇒  // ignore
  }
}
