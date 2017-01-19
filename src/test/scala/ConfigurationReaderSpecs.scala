package deeplabs.config.test


// Code here is to run tests all found in the following packages:
// - deeplabs.config
// 

import org.scalacheck.{Arbitrary, Gen, Properties,Prop}
import Arbitrary.arbitrary
import Gen.{containerOfN, choose, pick, mapOf, listOf, oneOf}
import Prop.{forAll, throws, AnyOperators}

object Config extends Properties("ConfigurationProperties") {

  // generate *all* permutations of the alphabet
  def alphabetStrings : List[String] = 
    ('a' to 'z').inits.map(_.mkString).toList

  def genMap(values: List[String]): Gen[Map[String,String]] = for {
     keys <- containerOfN[List,Int](values.size, arbitrary[Int])
   } yield keys.map(_.toString).zip(values).toMap

  property("parsing String keys") = 
    forAll(genMap(alphabetStrings)) {
      map ⇒ 
        if (map.isEmpty) {
          true 
        } else {
          val c = deeplabs.config.Config(map)
          val (key, _) = map.head
          c.parse[String](key) == c.parse[String](key)
        }
    }
}
