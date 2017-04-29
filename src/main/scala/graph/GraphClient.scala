package graph

import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish,Subscribe}

import cats._, data._

object GraphClient {
  def props : Reader[String,Props] =
    Reader{ (name: String) ⇒ Props(classOf[GraphClient], name) }

  case class Publish(msg: String) 
  case class Message(from: String, text: String) 
}

class GraphClient(name: String) extends Actor with ActorLogging {
  val mediator = DistributedPubSub(context.system).mediator
  val topic = "chatroom"

  override def preStart() : Unit = {
    mediator ! Subscribe(topic, self) 
  }
  
  def receive = {
    case GraphClient.Publish(msg) ⇒ mediator ! Publish(topic, GraphClient.Message(name, msg))
    case GraphClient.Message(from, text) ⇒
      val direction = if (sender == self) ">>>" else s"<< $from:"
      log.info(s"$name $direction $text")
  }
}
