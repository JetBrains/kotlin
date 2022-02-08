#!/usr/bin/env bash

# See http://yann.lecun.com/exdb/mnist/

MNIST_TARGET_DIRECTORY="`pwd`/build/3rd-party/MNIST"

echo "Downloading MNIST databases into $MNIST_TARGET_DIRECTORY ..."

mkdir -p $MNIST_TARGET_DIRECTORY
cd $MNIST_TARGET_DIRECTORY

wget -nv -N http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz
wget -nv -N http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz
wget -nv -N http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz
wget -nv -N http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz

gunzip -fk *.gz
