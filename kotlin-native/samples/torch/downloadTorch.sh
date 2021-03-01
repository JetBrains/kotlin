#!/usr/bin/env bash

KONAN_USER_DIR=${KONAN_DATA_DIR:-"$HOME/.konan"}
TH_TARGET_DIRECTORY="$KONAN_USER_DIR/third-party/torch"
NO_CUDA=true # set to false for GPU support

if [ ! -d $TH_TARGET_DIRECTORY/include/THNN ]; then
    echo "Installing Torch into $TH_TARGET_DIRECTORY ..."

    mkdir -p build/3rd-party
    cd build/3rd-party

    git clone https://github.com/pytorch/pytorch.git
    # Current pytorch master fails the build so we need to checkout a correct revision.
    cd pytorch && git checkout 310c3735b9eb97f30cee743b773e5bb054989edc^ && cd ../

    mkdir build_torch
    cd build_torch

    cmake -DNO_CUDA=$NO_CUDA ../pytorch/aten
    make
    make DESTDIR=$TH_TARGET_DIRECTORY install

    cd $TH_TARGET_DIRECTORY

    # remove 'usr/local' prefix produced by make:
    mv usr/local/* .
    rm -d usr/local usr

    # hack to solve "fatal error: 'generic/THNN.h' file not found" when linking, -I$<DIR>/include/THNN did not work
    cp include/THNN/generic/THNN.h include/TH/generic/THNN.h
fi
