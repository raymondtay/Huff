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
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Huff extends App {
  import deeplabs.http._
  import deeplabs.config._

  val systemEnvConfig = Config(sys.env)

  import Validator._
  val akkaHttpConfig = 
    validate(
      systemEnvConfig.parse[String]("DL_HTTP_ADDRESS").toValidatedNel,
      systemEnvConfig.parse[Int]("DL_HTTP_PORT").toValidatedNel)(AkkaHttpConfig.apply)

  //
  // The cluster node is only started when the configuration is 
  // valid and correct; otherwise, the logs would capture
  //
  for {
    c <- akkaHttpConfig
  } {
    println(s"--${c}")
    implicit val actorSystem = ActorSystem("huffsystem", ConfigFactory.load())
    implicit val actorMaterializer = ActorMaterializer()
    actorSystem.actorOf(Props[HuffListener], name = "HuffListener")
    val bindingF = Http().bindAndHandle(Routes.route, c.hostname, c.listeningPort)
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
