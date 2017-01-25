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
