package deeplabs.cluster

import scala.language.postfixOps
import scala.collection.JavaConversions._
import java.net.{NetworkInterface,InetAddress, UnknownHostException}

object ContainerHostIp {

  def getIpByHostname(host: String) : Either[UnknownHostException, String] = 
  try {
    Right(InetAddress.getByName(host).getHostAddress)
  } catch {
    case e : UnknownHostException ⇒ Left(e)
  }

  def load(interfaceName : String = "eth0") : Option[String] = {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    val interface = interfaces find (_.getName equals interfaceName)
    interface flatMap {
      inet ⇒ 
        inet.getInetAddresses find (_ isSiteLocalAddress) map (_ getHostAddress)
    }
  }

}

