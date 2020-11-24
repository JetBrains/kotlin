#!/bin/sh
set -eou pipefail

CONTAINER_NAME=kotlin-toolchain-builder
IMAGE_NAME=kotlin-toolchain-builder
TARGET=$1
VERSION=$2

if ! "docker ps -a | grep $CONTAINER_NAME";
then
  echo "Removing previous container..."
  docker stop $CONTAINER_NAME
  docker rm $CONTAINER_NAME
  echo "Done."
fi

echo "Running build script in container..."
docker run -it -v "$PWD"/artifacts:/artifacts --env TARGET="$TARGET" --env VERSION="$VERSION" --name=$CONTAINER_NAME $IMAGE_NAME
echo "Done."