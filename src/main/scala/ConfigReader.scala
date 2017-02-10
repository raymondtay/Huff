package deeplabs.config

// 
// Code here is mostly for reading values 
// from configuration files, system ENV variables
// 
// @author: Raymond Tay
// @date  : 19 Jan 2017
// 

import scala.concurrent.duration.{Duration,FiniteDuration}
import cats._
import cats.data._
import cats.implicits._
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

trait Read[A] {
  def read(data : String) : Option[A]
}

object Read {
  def apply[A](implicit A: Read[A]) : Read[A] = A

  implicit val stringRead: Read[String] = 
    new Read[String]{def read(s: String) : Option[String] = Some(s)}

  implicit val booleanRead: Read[Boolean] =
    new Read[Boolean] {
      def read(s:String) : Option[Boolean] = {
        s.toLowerCase match {
          case "true"  ⇒  Some(true)
          case "false" ⇒  Some(false)
          case "yes"   ⇒  Some(true)
          case "no"    ⇒  Some(false)
          case "1"     ⇒  Some(true)
          case "0"     ⇒  Some(false)
          case s       ⇒  None
        }
      }
    }

  implicit val intRead: Read[Int] = 
    new Read[Int]{
      def read(s: String) : Option[Int] = {
        if (s.matches("-?[0-9]+$")) Some(s.toInt)
        else None
      }
    }

  // Note: `Duration` parses the input string based on the
  // Scala 2.11.8's convention see [[scala.concurrent.duration.Duration]]
  // and we make no further checks, thereafter.
  implicit val finiteDurationRead : Read[FiniteDuration] = 
    new Read[FiniteDuration] {
      def read(s: String) : Option[FiniteDuration] = {
        try {
          val d = Duration(s)
          Some(FiniteDuration(d.length, d.unit))
        } catch {
          case e: NumberFormatException ⇒ None
        }
      } 
    }
}

sealed abstract class ConfigError
final case class MissingConfig(field : String) extends ConfigError
final case class ParseError(field: String) extends ConfigError

case class Config(map : Map[String,String]) {
  def parse[A : Read](key: String) : Validated[ConfigError, A] = 
    map.get(key) match {
      case None ⇒ Invalid(MissingConfig(key)) 
      case Some(value) ⇒ 
        Read[A].read(value) match {
          case None ⇒ Invalid(ParseError(key))
          case Some(a) ⇒ Valid(a)
        }
    }
}

case class HuffHeartbeatConfig(
  initialDelay : FiniteDuration,
  interval     : FiniteDuration,
  message : String
)

// de-serialize directly from `application.conf`
case class HeartbeatConfig(
  server_ip : String,
  initial_delay: String,
  interval : String
)

// @see section `huff.consul` in `application.conf`
case class HuffConsulConfig(enabled : Boolean, hostname:String, port: Int, serviceName: String, serviceTagName: String)

case class HuffConfig(
  clusterName: String,
  clusterPort : Int,
  clusterAddress : String, 
  hostname: String, 
  listeningPort: Int)

object ScalaCfg {
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
  import com.github.andr83.scalaconfig._
  def heartBeatCfg(config : com.typesafe.config.Config) = {
    val c = config.as[HeartbeatConfig]("huff.heartbeat.message")
    val m = config.as[Map[String,String]]("huff.heartbeat.settings")
    m + ("message" -> c.asJson.noSpaces.toString)
  }
  def consulCfg(config : com.typesafe.config.Config) = {
    config.as[Map[String,String]]("huff.consul")  
  }
}
// 
// This object contains some helper functions which allows
// the developer to aggregate all errors into the non-empty list
// 
object Validator {

  def validate[E : Semigroup, A, B, C](
    a : Validated[E, A], 
    b : Validated[E, B]
    )(f : (A, B) ⇒ C) : Validated[E,C] = 
      (a, b) match {
        case (Valid(_a), Valid(_b)) ⇒ Valid(f(_a,_b))
        case (Valid(_), wrongV@Invalid(_)) ⇒ wrongV
        case (wrongV@Invalid(_), Valid(_)) ⇒ wrongV
        case (Invalid(e1), Invalid(e2))    ⇒ Invalid(Semigroup[E].combine(e1, e2))
      }

  def getHuffConsulConfig(config: Config) : ValidatedNel[ConfigError, HuffConsulConfig] = 
    Apply[ValidatedNel[ConfigError, ?]].map5(
      config.parse[Boolean]("enabled").toValidatedNel,
      config.parse[String]("host").toValidatedNel,
      config.parse[Int]("port").toValidatedNel, 
      config.parse[String]("service_name").toValidatedNel,
      config.parse[String]("tag_name").toValidatedNel) {
        case (enabled, consulHostname, consulPort, serviceName, serviceTagName) ⇒ 
          HuffConsulConfig(enabled, consulHostname, consulPort, serviceName, serviceTagName)
      }

  def getHuffHeartbeatConfig(config: Config) : ValidatedNel[ConfigError, HuffHeartbeatConfig] = 
    Apply[ValidatedNel[ConfigError, ?]].map3(
      config.parse[FiniteDuration]("initialdelay").toValidatedNel,
      config.parse[FiniteDuration]("interval").toValidatedNel,
      config.parse[String]("message").toValidatedNel) {
        case (initialDelay, interval, message) ⇒ 
          HuffHeartbeatConfig(initialDelay, interval, message)
      }

  def getHuffConfig(config: Config) : ValidatedNel[ConfigError, HuffConfig] = 
    Apply[ValidatedNel[ConfigError, ?]].map5(
      config.parse[String] ("DL_CLUSTER_NAME").toValidatedNel,
      config.parse[Int]    ("DL_CLUSTER_PORT").toValidatedNel,
      config.parse[String] ("DL_CLUSTER_ADDRESS").toValidatedNel,
      config.parse[String] ("DL_HTTP_ADDRESS").toValidatedNel,
      config.parse[Int]    ("DL_HTTP_PORT").toValidatedNel) {
        case (clusterName, clusterPort, clusterAddress, httpAddr, httpPort) ⇒ 
          HuffConfig(clusterName, clusterPort, clusterAddress, httpAddr, httpPort)
      }

} 

