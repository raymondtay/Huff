# Huff

Huff is a peer-to-peer clustering solution that provides the following functionalities:

- Fault tolerant 
- Scalable ReST APIs
- Coordinates the transaction on the behest of the requester

# Running locally

This assumes you have the codebase, and then from the commandline terminal
you would execute the following:
```
sbt -DJMX_REMOTE_PORT=9999 -DJMX_REMOTE_SSL=false -DJMX_REMOTE_AUTHENTICATE=false run
```

*TODO*:
- Insert code snippets w.r.t how to run the server from the `sbt`
- Insert code snippets w.r.t how to run the packaging from `sbt`, including executing


