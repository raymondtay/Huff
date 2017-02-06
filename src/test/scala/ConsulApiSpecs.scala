package deeplabs.cluster

import org.scalacheck.{Arbitrary, Gen, Properties,Prop}
import Arbitrary.arbitrary
import Gen.{containerOfN, choose, pick, mapOf, listOf, oneOf}
import Prop.{forAll, throws, AnyOperators}
import deeplabs.config.{ConfigError, ParseError, MissingConfig}
import com.typesafe.config.{Config, ConfigFactory}
import org.specs2.specification._
import org.specs2._
import scala.language.postfixOps

class ConsulIOApiSpecs extends Specification with BeforeAll with AfterAll with ScalaCheck {

  private[this] var consulproc : sys.process.Process = _

  override def beforeAll() = {
    import sys.process._
    consulproc = stringToProcess("consul agent -dev").run()
  }

  override def afterAll() = consulproc.destroy()

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

  implicit val genArbitraryConfig = Arbitrary(Gen.oneOf(testConfs))
  def is = s2"""
     All consul.io configurations will be validated and represented as either Valid or Invalid values in `cats.data.Validated` $configuratorValidator 
  """

  def configuratorValidator = 
    prop(
      (conf:com.typesafe.config.Config) ⇒ 
      ConsulApi(conf).isValid match {
        case true ⇒ true
        case false ⇒  // when its invalid, then it means it picked up a configuration that couldn't be parsed properly
          ConsulApi(conf).isInvalid match {
            case true ⇒ true 
            case false ⇒ false // its not likely we would EVER hit this in this testing as it would mean its a REAL error in CATS
          }
      }
    )
    
}

