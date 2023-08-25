#!/bin/bash

function do_set {
    pushd "$(dirname $1)"
    ./$(basename $1)
    popd
}

do_set kotlin-native/platformLibs/src/platform/ios/set_depends.sh
do_set kotlin-native/platformLibs/src/platform/osx/set_depends.sh
do_set kotlin-native/platformLibs/src/platform/tvos/set_depends.sh
do_set kotlin-native/platformLibs/src/platform/watchos/set_depends.sh
