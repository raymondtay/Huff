package deeplabs.cluster

import scala.concurrent.duration._

import org.scalatest._
import akka.actor._
import akka.testkit._
import com.typesafe.config._

class HeartbeatSpecs extends WordSpec with BeforeAndAfterAll { 
  import deeplabs.config._
  import Validator._

  val testConf: com.typesafe.config.Config =
    ConfigFactory.parseString("""
      akka {
        remote.netty.tcp.port=2444
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
        stdout-loglevel = "WARNING"
        actor {
          default-dispatcher {
            executor = "fork-join-executor"
            fork-join-executor {
              parallelism-min = 8
              parallelism-factor = 2.0
              parallelism-max = 8
            }
          }
        }
      }
      huff {
        heartbeat {
          settings {
            initialdelay = 0 ms
            interval = 1000 ms
          }
          message  : {
            "server_ip"     : localhost
            "initial_delay" : 0 ms
            "interval"      : 1000 ms
          }
        }
      }
                                                    """)

  val actorSystem = ActorSystem("test-heart-beat", testConf)

  override def afterAll() {
    TestKit.shutdownActorSystem(actorSystem)
  }
  "A heartbeat actor" must {

    "send has no reply" in {
      new TestKit(actorSystem) with ImplicitSender {
        for {
          hb <- getHuffHeartbeatConfig(Config(ScalaCfg.heartBeatCfg(testConf)))
        } {
          val actor = actorSystem.actorOf(Props(classOf[Heartbeat], hb.initialDelay, hb.interval, hb.message))
          actor ! "Tick"
          expectNoMsg
        }
      }
    }
  }
}

