#!/bin/bash
set -eou pipefail

CONTAINER_NAME=kotlin-qemu-builder
IMAGE_NAME=kotlin-qemu-builder


docker ps -a | grep $CONTAINER_NAME > /dev/null \
  && docker stop $CONTAINER_NAME 1> /dev/null \
  && docker rm $CONTAINER_NAME 1> /dev/null

docker run -it -v "$PWD"/out:/output --name=$CONTAINER_NAME $IMAGE_NAME
