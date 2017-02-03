package deeplabs.cluster

import deeplabs.config.{Config, ScalaCfg, Validator}

//
// Factory object to retrieve the interface object to the
// Consul.io service.
// Make sure the `huff.consul.hostname` and `huff.consul.port`
// is correct and `huff.consul.enabled=true` holds.
//
object ConsulApi {
  import Validator._
  def apply(config : com.typesafe.config.Config) = 
    for {
      config <- getHuffConsulConfig(Config(ScalaCfg.consulCfg(config)))
    } yield (config, com.orbitz.consul.Consul.builder().withUrl(s"http://${config.hostname}:${config.port}").build())

}


