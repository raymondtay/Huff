package deeplabs.config

// 
// Code here is mostly for reading values 
// from configuration files, system ENV variables
// 
// @author: Raymond Tay
// @date  : 19 Jan 2017
// 

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

  implicit val intRead: Read[Int] = 
    new Read[Int]{
      def read(s: String) : Option[Int] = {
        if (s.matches("-?[0-9]+$")) Some(s.toInt)
        else None
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

case class AkkaHttpConfig(hostname: String, listeningPort: Int)

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
} 

