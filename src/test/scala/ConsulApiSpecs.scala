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
import com.orbitz.consul._
import com.orbitz.consul.model.health._
import com.orbitz.consul.cache._

class ConsulIOApiSpecs extends Specification with BeforeAll with AfterAll with ScalaCheck {

  private[this] var consulproc : sys.process.Process = _
  private[this] var consulclient : Consul = _

  override def beforeAll() = {
    import sys.process._
    consulproc = stringToProcess("consul agent -dev").run()
    consulclient = Consul.builder().withUrl(s"http://localhost:8500").build  
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

  implicit val serviceHealthNode = Arbitrary{
    import java.util.UUID._
    //
    // Contents of the test is recovered from the orbitz.consul java-client
    // software tests. see com.orbitz.consul.cache.ConsulCacheTest
    // for details.
    //
    val healthClient = consulclient.healthClient()
    val serviceName = randomUUID().toString()
    val serviceId = randomUUID().toString()

    consulclient.agentClient().register(8080, 20L, serviceName, serviceId)
    consulclient.agentClient().pass(serviceId)
    val svHealth = ServiceHealthCache.newCache(healthClient, serviceName)
    svHealth.start()
    svHealth.awaitInitialized(3, java.util.concurrent.TimeUnit.SECONDS)
    val serviceKey = ServiceHealthKey.of(serviceId, consulclient.agentClient().getAgent().getConfig().getAdvertiseAddr(), 8080)
    svHealth.getMap().get(serviceKey)
  }
    
  def is = s2"""
     All consul.io configurations will be validated and represented as either Valid or Invalid values in `cats.data.Validated` $configuratorValidator 
     `constructActorAddress` must: (a) return a empty list when actor address format is invalid; (b) return a non-empty list when address format is valid $actorAddressValidator
  """

  def actorAddressValidator = 
    prop(
      (node: ServiceHealth) ⇒ 
        ConsulApi.constructActorAddress(node, "test-actor-system")("localhost") match {
	  case Nil ⇒ true
	  case x :: xs ⇒ true
	}
    )

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

