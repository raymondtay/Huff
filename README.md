# Huff

Huff is a peer-to-peer clustering solution that provides the following functionalities:

- Fault tolerant 
- Scalable ReST APIs
- Coordinates the transaction on the behest of the requester

# Running locally

The following variables affect how the cluster is to be run 
either locally or distributed across nodes:

- `DL_CLUSTER_ADDRESS` e.g. `localhost`, `0.0.0.0`
- `DL_CLUSTER_PORT` e.g `2551` (this needs to be `distinct` if cluster nodes are run locally)
- `DL_HTTP_ADDRESS` e.g. `localhost`, `0.0.0.0`
- `DL_HTTP_PORT`    e.g. `8080`

- `JMX_REMOTE_PORT`
- `JMX_REMOTE_SSL`
- `JMX_REMOTE_AUTHENTICATE`

## Running locally via `sbt`

If its done properly, you would have a 2-node cluster communicating
internal via ports `2551` and `2551` and the `Http` service would be 
available via `8080` and `8081`.

### An example ...

To run a 2-node cluster node on your local machine, you need to perform the following
- Open 2 terminals and navigate to the code base
- In 1st terminal, activate the cluster: `sbt run`
- In 2nd terminal, as we cannot launch another process to bind to the same ports, so we have\
  to make sure they run on different ports. E.g.
  >  export DL_CLUSTER_ADDRESS=localhost
  >  export DL_CLUSTER_PORT=2552
  >  export DL_HTTP_ADDRESS=localhost
  >  export DL_HTTP_PORT=8081
  >  sbt -DJMX_REMOTE_PORT=9999 run

### Http Test

At this point, you should have the 2-node cluster running locally
and you can perform the test as follows via `curl` : `curl -v http://localhost:8080/status`
which should return a message `System-node OK`.

*TODO*:
- Insert code snippets w.r.t how to run the server from the `sbt`
- Insert code snippets w.r.t how to run the packaging from `sbt`, including executing


