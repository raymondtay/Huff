#!/bin/bash

environment=$1
HUFF_NAME=huff # docker allows only lowercase names for images


echo "[DeepLabs] You have chosen to activate the $environment mode..."

if test -f ./Dockerfile
then
  echo "About to build VM image: ${HUFF_NAME}"
  docker build -t ${HUFF_NAME} .
else
  echo "Unable to find `Dockerfile` here..."
  exit
fi

