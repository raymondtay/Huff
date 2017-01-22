package deeplabs.cluster

import scala.language.postfixOps
import scala.collection.JavaConversions._
import java.net.NetworkInterface

object ContainerHostIp {
        
  def load() : Option[String] = {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    val interface = interfaces find (_.getName equals "eth0")
    interface flatMap {
      inet ⇒ 
        inet.getInetAddresses find (_ isSiteLocalAddress) map (_ getHostAddress)
    }
  }

}

