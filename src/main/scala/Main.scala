package deeplabs.cluster

/** 
 Huff -

  The cluster launcher that loads the `Http` code
  to serve client requests.
 */

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.cluster.{Cluster, ClusterEvent}
import ClusterEvent._
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Huff extends App {

  //val port = sys.props.getOrElse("DL_CLUSTER_PORT", default="2551")
  //val addr = sys.props.getOrElse("DL_CLUSTER_ADDRESS", default="127.0.0.1")
  /*
  val config = 
    ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${port.toInt}")
    .withFallback(ConfigFactory.load())

  implicit val actorSystem = ActorSystem("huffsystem", config)
  implicit val actorMaterializer = ActorMaterializer()

  actorSystem.actorOf(Props[HuffListener], name = "HuffListener")
  */
  import deeplabs.config._

  // this is a local config
  val config = 
    Config(Map("hostname" -> "localhost", "port" -> "2345"))

  // this is the global environmental configuration
  val systemEnvConfig = 
    Config(sys.env)

  // this is the properties configured for this JVM 
  val systemConfig = Config(sys.props.toMap)

  val (url, port) = (config.parse[String]("hostname"), config.parse[Int]("port"))

  import Validator._
  val akkaConfig = 
    validate(
      config.parse[String]("hostname").toValidatedNel,
      config.parse[Int]("port").toValidatedNel)(AkkaConfig.apply)

  for {
    c <- akkaConfig
  } println(c)
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
