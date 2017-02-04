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

  // Examples are extracted from [https://www.ietf.org/rfc/rfc2732.txt]
  val validAddresses =
    Gen.frequency(
      (10, "localhost"),
      (10,"FEDC:BA98:7654:3210:FEDC:BA98:7654:3210"),
      (10,"1080:0:0:0:8:800:200C:4171"),
      (10,"3ffe:2a00:100:7031::1"),
      (10,"1080::8:800:200C:417A"),
      (10,"::192.9.5.5"),
      (10,"::FFFF:129.144.52.38"),
      (10,"2010:836B:4179::836B:4179"))

  val genInvalidAddresses =
    oneOf(List("0.0.0.0.0"))

  property("any node with valid ipv4 should return an String") =
    forAll(validAddresses) {
      host:String ⇒
        ContainerHostIp.getIpByHostname(host).isRight match {
	  case true ⇒ true
	  case false ⇒ false
	}
    }

  property("any node with invalid ipv4 addresses should throw an 'java.net.UnknownHostException'") =
    forAll(genInvalidAddresses) {
      host:String ⇒
        ContainerHostIp.getIpByHostname(host).isLeft match {
	  case true ⇒ true
	  case false ⇒ false
	}
    }
}

