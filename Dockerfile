#
# Scala and sbt Dockerfile
#
# https://github.com/hseeberger/scala-sbt
#

# Pull base image
FROM  openjdk:8

ENV SCALA_VERSION 2.11.8
ENV SBT_VERSION 0.13.13

# Scala expects this file
RUN touch /usr/lib/jvm/java-8-openjdk-amd64/release

# Install Scala
## Piping curl directly in tar
RUN \
  curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

# Define working directory
WORKDIR /root

COPY ./target/universal/huff-0.9.tgz .

RUN \
  tar xvfz ./huff-0.9.tgz

# Getting the Cluster environment ready.
# Running the process as a foreground
WORKDIR /root/huff-0.9
ENV DL_CLUSTER_NAME huffcluster
ENV DL_CLUSTER_ADDRESS localhost
ENV DL_CLUSTER_PORT 2551
# a string value like "" or "<host/ip>:<port>"
ENV DL_CLUSTER_SEED_NODE ""
ENV IS_SEED true
ENV DL_HTTP_ADDRESS localhost
ENV DL_HTTP_PORT    8080
# Commented out the entry point
ENTRYPOINT ["./bin/huff"]

