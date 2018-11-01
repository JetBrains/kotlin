#!/usr/bin/env bash

KONAN_TOOLCHAIN_VERSION=8
SDKS="macosx iphoneos iphonesimulator"
TARBALL_macosx=target-sysroot-$KONAN_TOOLCHAIN_VERSION-macos_x64
TARBALL_iphoneos=target-sysroot-$KONAN_TOOLCHAIN_VERSION-ios_arm64
TARBALL_iphonesimulator=target-sysroot-$KONAN_TOOLCHAIN_VERSION-ios_x64
TARBALL_watchos=target-sysroot-$KONAN_TOOLCHAIN_VERSION-watchos_arm32
TARBALL_watchsimulator=target-sysroot-$KONAN_TOOLCHAIN_VERSION-watchos_x64
TARBALL_xcode=target-toolchain-$KONAN_TOOLCHAIN_VERSION-macos_x64
OUT=`pwd`

for s in $SDKS; do
  p=`xcrun --sdk $s --show-sdk-path`
  p=`grealpath $p`
  tarball_var=TARBALL_${s}
  tarball=${!tarball_var}
  echo "Packing SDK $s as $OUT/$tarball.tar.gz..."
  $SHELL -c "tar czf $OUT/$tarball.tar.gz -C $p -s '/^\./$tarball/'  ."
done

t=`xcrun -f ld`
t=`dirname $t`
t=`grealpath $t/../..`
tarball=$TARBALL_xcode
echo "Packing toolchain $OUT/$tarball.tar.gz..."
$SHELL -c "tar czf $OUT/$tarball.tar.gz -C $t -s '/^\./$tarball/'  ."
