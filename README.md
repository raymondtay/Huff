# Huff

Huff is a peer-to-peer clustering solution that provides the following functionalities:

- Fault tolerant 
- Scalable ReST APIs
- Coordinates the transaction on the behest of the requester

# Updates

- 23 Jan 2017 
-- Potential memory leak from a DDOS targeting `akka-http` 
   was discovered [here](http://akka.io/news/2017/01/23/akka-http-10.0.2-security-fix-released.html)
-- Bumping version of `akka-http` from `10.0.1` to `10.0.2`

# Running locally

The following variables affect how the cluster is to be run 
either locally or distributed across nodes:

- `DL_CLUSTER_NAME`      # a name for the cluster  e.g. huffcluster
- `DL_CLUSTER_ADDRESS`   # valid hostname or ipv4/v6 e.g. `localhost`, `0.0.0.0`
- `DL_CLUSTER_SEED_NODE` # Must be pointing to a valid url of the cluster e.g. `localhost:2551`, `0.0.0.0:2551` 
- `DL_CLUSTER_PORT`      # Port number of cluster is listening e.g `2551` (this needs to be `distinct` if cluster nodes are run locally i.e. w/o `docker`)
- `DL_HTTP_ADDRESS`      # valid hostname or ipv4/v6 e.g. `localhost`, `0.0.0.0`
- `DL_HTTP_PORT`         # Port number of http servie e.g. `8080`
- `IS_SEED`              # when `true`,`yes` or `1` then we will launch this as a seed-node otherwise its converse means not a seed-node
- `JMX_REMOTE_PORT`
- `JMX_REMOTE_SSL`
- `JMX_REMOTE_AUTHENTICATE`

## Running locally via `sbt`

When running locally, a user process typically cannot `bind` to a local port _twice_ and therefore
we need to bind to different ports. If its done properly, you would have a 2-node cluster communicating
internal via ports `2551` and `2552` and the `Http` service would be 
available via `8080` and `8081`.

### An example ...

To run a 2-node cluster node on your local machine, you need to perform the following
- Open 2 terminals and navigate to the code base
- In 1st terminal, do the following:
 
    > export DL_HTTP_ADDRESS=localhost
    
    > export DL_HTTP_PORT=8080
    
    > codebase_dir #> sbt run

- In 2nd terminal, as we cannot launch another process to bind to the same ports, so we have
  to make sure they run on different ports. E.g.
  
    >  export DL_CLUSTER_ADDRESS=localhost
    
    >  export DL_CLUSTER_PORT=2552
    
    >  export DL_HTTP_ADDRESS=localhost
    
    >  export DL_HTTP_PORT=8081
    
    >  codebase_dir #> sbt -DJMX_REMOTE_PORT=9999 run


### Http Test

At this point, you should have the 2-node cluster running locally
and you can perform the test as follows via `curl` : `curl -v http://localhost:8080/status`
which should return a message `System-node OK`.

# Running via `Docker`  

The presumption is that you should have installed `docker` on the target machine
or machines you plan to run this cluster service in. You can run this service in two
modes:

- As a `development` machine
- As a `production` machine (To be announced at a later date)

In either modes, it is important to set `coverageEnabled` to `false` 
in `sbt` before you package `Huff` for service e.g. this is an example of what should happen 
when building from source:
```
set coverageEnabled := false
[info] The new value will be used by *:libraryDependencies, compile:compile::scalacOptions
[info] Reapplying settings...
[info] Set current project to Huff (in build file:/Users/tayboonl/Huff/)
```
## Run-As-A-Development environment ( meant for *local* development only ) 

Using `docker` as the approach, we would first need a docker container
containing the `Huff` cluster service. Follow the steps as prescribed:

- Find the `Dockerfile` and run the command `docker build -t huff .`
- Verify that the image is indeed present by running `docker images`
- Run the `seed`-node-1 of the cluster via `docker run -t -i -p 2551:2551 -p 8080:8080 huff /bin/bash`
-- This should bring you into the container and then you run the container via `./bin/huff`
- Run the `seed`-node-2 of the cluster via `docker run -t -i -p 2552:2552 -p 8081:8080 huff /bin/bash`
-- This should bring you into the container and then you run the container via `./bin/huff`

At this point the cluster should be operational as you can see both containers communicating with 
one another.

Now, you need to re-run the `Http Test` again to make sure you can see the message `System-node OK`
being printed.



