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

  def getHuffConfig : ValidatedNel[ConfigError, HuffConfig] = 
    Apply[ValidatedNel[ConfigError, ?]].map6(
      systemEnvConfig.parse[String]("DL_CLUSTER_NAME").toValidatedNel,
      systemEnvConfig.parse[Int]("DL_CLUSTER_PORT").toValidatedNel,
      systemEnvConfig.parse[Boolean]("IS_SEED").toValidatedNel,
      systemEnvConfig.parse[String]("DL_CLUSTER_SEED_NODE").toValidatedNel,
      systemEnvConfig.parse[String]("DL_HTTP_ADDRESS").toValidatedNel,
      systemEnvConfig.parse[Int]("DL_HTTP_PORT").toValidatedNel) {
        case (clusterName, clusterPort, isSeed, dlClusterSeedNode, httpAddr, httpPort) ⇒ 
          val ip = ContainerHostIp.load() getOrElse "127.0.0.1"
          val seedNodeStrings = 
            dlClusterSeedNode.isEmpty match {
              case true ⇒ 
                Seq(s"""akka.cluster.seed-nodes += "akka.tcp://$clusterName@$ip:$clusterPort"""")
              case false ⇒ 
                Seq(s"""akka.cluster.seed-nodes += "akka.tcp://$clusterName@$ip:$clusterPort"""",
                    s"""akka.cluster.seed-nodes += "akka.tcp://$clusterName@$dlClusterSeedNode"""")
            }

          HuffConfig(clusterName, clusterPort, isSeed, seedNodeStrings, httpAddr, httpPort)
      }

  //
  // The cluster node is only started when the configuration is 
  // valid and correct; otherwise, the logs would capture
  //
  for {
    c <- getHuffConfig
  } {
    val data = DLLog(
      service_name = "Huff cluster",
      category     = "application",
      event_type   = "operation",
      message      = s"Starting up Http service: ${c.listeningPort}"
      )
    logger.info(data.asJson.noSpaces)
    implicit val actorSystem = 
      ActorSystem(c.clusterName, 
        ConfigFactory.load().
        withValue("akka.cluster.seed-nodes",
	  ConfigFactory.parseString(c.seedNodes.mkString("\n")) resolve() getList("akka.cluster.seed-nodes")).
        withValue("akka.remote.netty.tcp.hostname",
	  ConfigValueFactory.fromAnyRef(ContainerHostIp.load() getOrElse "127.0.0.1")).
        withValue("akka.remote.netty.tcp.port",
	  ConfigValueFactory.fromAnyRef(2551)).resolve()
      )
    implicit val actorMaterializer = ActorMaterializer()
    actorSystem.actorOf(Props[HuffListener], name = "HuffListener")
    val server = new RestServer()
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
