package deeplabs.config.test


// Code here is to run tests all found in the following packages:
// - deeplabs.config
// 

import org.scalacheck.{Arbitrary, Gen, Properties,Prop}
import Arbitrary.arbitrary
import Gen.{containerOfN, choose, pick, posNum, negNum, mapOf, listOf, oneOf}
import Prop.{forAll, throws, AnyOperators}
import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import deeplabs.config.{ConfigError, ParseError, MissingConfig}

object Config extends Properties("ConfigurationProperties") {

  // generate *all* permutations of the alphabet
  def alphabetStrings : List[String] = 
    ('a' to 'z').inits.map(_.mkString).toList

  // generate strings of test 
  def intStrings : List[String] = 
    (1 to 1000).map(_.toString).toList

  def someIntNsomeNonIntStrings = 
    Gen.frequency(
      (50, "12ab"),
      (50, choose(0,100).map(_.toString))
    )

  // generate Map[String,String]
  def genMap(values: List[String]): Gen[Map[String,String]] = for {
     keys <- containerOfN[List,Int](values.size, arbitrary[Int])
   } yield keys.map(_.toString).zip(values).toMap

  property("parsing map of K:String keys, V:Int values") = 
    forAll(genMap(alphabetStrings)) {
      map ⇒ 
        if (map.isEmpty) {
          true 
        } else {
          val c = deeplabs.config.Config(map)
          val (key, _) = map.head
          c.parse[String](key).isValid match {
	    case true ⇒ true
	    case false ⇒ false
	  }
        }
    }

  // using a frequency generator to `inject`
  // certain values at test-time.
  property("parsing map of K:Int keys, V:{non-Int,Int} values") = 
    forAll(someIntNsomeNonIntStrings) {
      s:String ⇒ 
        val c = deeplabs.config.Config(Map(1.toString -> s))
        c.parse[Int](1.toString) match {
          case Valid(v) ⇒ true // expecting this to be true
          case Invalid(ParseError(_)) ⇒ true // expecting this to be true
          case Invalid(MissingConfig(_)) ⇒  false // ignoring this since its covered 
        }
    }

  property("parsing map of K:Int keys, V:Int values") = 
    forAll(genMap(intStrings)) {
      map ⇒ 
        if (map.isEmpty) {
          true 
        } else {
          val c = deeplabs.config.Config(map)
          val (key, _) = map.head
          c.parse[Int](key).isValid match {
	    case true ⇒ true
	    case false ⇒ false
	  }
        }
    }

  property("missing keys in configuration should be detected") =
    forAll(genMap(alphabetStrings)) {
      map ⇒ 
        if (map.isEmpty) {
          true 
        } else {
          val invalidKey = 42.toString // this is an invalid key for our map
          val c = deeplabs.config.Config(map)
          c.parse[String](invalidKey).isInvalid match {
            case true ⇒ true
            case false ⇒ false
          }
        }
    }

  // value generator using a frequency alogrithm
  // so that there's a deterministic way to generate
  // random values at runtime.
  val allowedValuesGen = Gen.frequency(
    (4,"true"),
    (4,"false"),
    (3,"yes"),
    (3,"no"), 
    (2, "1"),
    (3, "0")
  )

  property("parsing errors in allowed values ∈ configuration should be detected") =
    forAll(allowedValuesGen) {
      s:String ⇒ 
        val c = deeplabs.config.Config(Map(1.toString -> s))
        c.parse[Boolean](1.toString).isValid match {
          case true ⇒ true
          case false ⇒ false
        }
    }

  property("valid keys with parsing errors in value ∈ configuration should be detected") =
    forAll(genMap(alphabetStrings)) {
      map ⇒ 
        if (map.isEmpty) {
          true 
        } else {
          val c = deeplabs.config.Config(map)
          val (key, _) = map.head
          c.parse[Boolean](key).isInvalid match {
            case true ⇒ true
            case false ⇒ false
          }
        }
    }

  property("validator.validate tests where tuple is valid") = {
    forAll {
      (x: String, y:String) ⇒ 
        implicit val nelSemigroup: cats.Semigroup[NonEmptyList[String]] = cats.SemigroupK[NonEmptyList].algebra[String]
        deeplabs.config.Validator.validate(Valid(x), Valid(y))((_:String) ++ (_:String)) == Valid(x++y)
    }
  }

}
