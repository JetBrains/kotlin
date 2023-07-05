#!/bin/bash
set -eou pipefail

CONTAINER_NAME=kotlin-toolchain-builder
IMAGE_NAME=kotlin-toolchain-builder
TARGET=$1
VERSION=$2
TOOLCHAIN_VERSION_SUFFIX="${3:-""}"

docker ps -a | grep $CONTAINER_NAME > /dev/null \
  && docker stop $CONTAINER_NAME > /dev/null \
  && docker rm $CONTAINER_NAME > /dev/null

echo "Running build script in container..."
docker run \
  --env TARGET="$TARGET" \
  --env VERSION="$VERSION" \
  --env TOOLCHAIN_VERSION_SUFFIX="$TOOLCHAIN_VERSION_SUFFIX" \
  --name=$CONTAINER_NAME $IMAGE_NAME

mkdir -p artifacts
docker cp kotlin-toolchain-builder:"/home/ct/x-tools/$TARGET-$VERSION-$TOOLCHAIN_VERSION_SUFFIX.tar.gz" artifacts/

echo "Done."