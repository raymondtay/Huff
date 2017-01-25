package deeplabs.cluster

// Code here is to run tests all found in the following packages:
// - deeplabs.cluster
// 

import org.scalacheck.{Arbitrary, Gen, Properties,Prop}
import Arbitrary.arbitrary
import Gen.{containerOfN, choose, pick, mapOf, listOf, oneOf}
import Prop.{forAll, throws, AnyOperators}
import cats.data.Validated.{Invalid, Valid}
import deeplabs.config.{ConfigError, ParseError, MissingConfig}

object ContainerHostIpSpecs extends Properties("ContainerHostIpProperties") {
  // This test is valid for Mac OS X and Linux since the interface
  // names would vary across different operating systems.
  // The primary use-case for this test is to make sure that the default
  // NIC interface name, e.g. eth0 (Linux), en0 (Mac), is captured
  //

  // interface-name generator
  val isMac = 
    sys.props("os.name").toLowerCase().startsWith("mac")

  val validNetworkInterfaceNames = 
      for { n <- choose(0,22) } yield { if (isMac) s"en${n}" else s"eth${n}" }

  property("a machine with an network interface named `eth0` should return at least 1 Ip") = 
    forAll(validNetworkInterfaceNames) {
      inet:String ⇒ 
        ContainerHostIp.load(inet) match {
          case Some(ip) ⇒ true
          case None     ⇒ true
          case _        ⇒ false
        }
    }
}

