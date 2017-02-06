package deeplabs.cluster

import org.scalacheck.{Arbitrary, Gen, Properties,Prop}
import Arbitrary.arbitrary
import Gen.{containerOfN, choose, pick, mapOf, listOf, oneOf}
import Prop.{forAll, throws, AnyOperators}
import cats.data.Validated.{Invalid, Valid}
import deeplabs.config.{ConfigError, ParseError, MissingConfig}
import com.typesafe.config.{Config, ConfigFactory}

//
// Downside of this test for now is that it requires a 
// valid consul.io agent to be available running on the localhost
// or whatever the IP addresses defined in the configuration
// section `huff.consul.host`.
// 
object ConsulApiSpecs extends Properties("ConsulApiProperties") {

  // TODO
  // - Factor this test so that it becomes part of the setup
  //   and remove the teardown from the buildfile.
  //
  new Thread(new Runnable() {
    override def run() : Unit = {
      import sys.process._
      import scala.language.postfixOps
      s"consul agent -dev"! 
    }
  }).start()

  val testConfs : Seq[com.typesafe.config.Config] = Seq(
    ConfigFactory.parseString("""
    huff.consul.enabled = true
    huff.consul.host = "localhost"
    huff.consul.port = 8500
    huff.consul.service_name = "test-service-name"
    huff.consul.tag_name = "test-tag"
    """),

    ConfigFactory.parseString("""
    huff.consul.enabled = false
    huff.consul.host = "localhost"
    huff.consul.port = 8500
    huff.consul.service_name = "test-service-name"
    huff.consul.tag_name = "test-tag"
    """),

    ConfigFactory.parseString("""
    huff.consul.enabled = false
    huff.consul.host = "localhost"
    huff.consul.port = 8500
    huff.consul.service_name = "test-service-name" 
    """),

    ConfigFactory.parseString("""
    huff.consul.enabled = true
    huff.consul.host = "localhost"
    huff.consul.port = 8500
    huff.consul.service_name = "test-service-name"
    huff.consul.tag_name = "test-tag"
    """)
  )

  property("A valid/invalid configuration that points to a valid/invalid consul.io agent should be caught by the checks.") = 
    forAll(oneOf(testConfs)) {
      conf:com.typesafe.config.Config ⇒ 
      ConsulApi(conf).isValid match {
        case true ⇒ true
        case false ⇒  // when its invalid, then it means it picked up a configuration that couldn't be parsed properly
          ConsulApi(conf).isInvalid match {
            case true ⇒ true 
            case false ⇒ false // its not likely we would EVER hit this in this testing as it would mean its a REAL error in CATS
          }
      }
    }
}

