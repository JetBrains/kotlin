#!/usr/bin/env bash
set -e

# "brew install coreutils" for grealpath.
KONAN_TOOLCHAIN_VERSION=xcode_15.1_15C65
SDKS="macosx iphoneos iphonesimulator appletvos appletvsimulator watchos watchsimulator"
TARBALL_macosx=target-sysroot-$KONAN_TOOLCHAIN_VERSION-macosx
TARBALL_iphoneos=target-sysroot-$KONAN_TOOLCHAIN_VERSION-iphoneos
TARBALL_iphonesimulator=target-sysroot-$KONAN_TOOLCHAIN_VERSION-iphonesimulator
TARBALL_appletvos=target-sysroot-$KONAN_TOOLCHAIN_VERSION-appletvos
TARBALL_appletvsimulator=target-sysroot-$KONAN_TOOLCHAIN_VERSION-appletvsimulator
TARBALL_watchos=target-sysroot-$KONAN_TOOLCHAIN_VERSION-watchos
TARBALL_watchsimulator=target-sysroot-$KONAN_TOOLCHAIN_VERSION-watchsimulator
TARBALL_xcode=target-toolchain-$KONAN_TOOLCHAIN_VERSION
TARBALL_xcode_addon=xcode-addon-$KONAN_TOOLCHAIN_VERSION
OUT=`pwd`

for s in $SDKS; do
  p=`xcrun --sdk $s --show-sdk-path`
  p=`grealpath $p`
  tarball_var=TARBALL_${s}
  tarball=${!tarball_var}
  echo "Packing SDK $s as $OUT/$tarball.tar.gz..."
  $SHELL -c "tar czf $OUT/$tarball.tar.gz -C $p -s '/^\./$tarball/HS'  ."
done

t=`xcrun -f ld`
t=`dirname $t`
t=`grealpath $t/../..`
tarball=$TARBALL_xcode
echo "Packing toolchain $OUT/$tarball.tar.gz..."
$SHELL -c "tar czf $OUT/$tarball.tar.gz -C $t -s '/^\./$tarball/HS'  ."

t=`xcrun -f bitcode-build-tool`
t=`dirname $t`
t=`grealpath $t/..`
tarball=$TARBALL_xcode_addon
echo "Packing additional tools $OUT/$tarball.tar.gz..."
$SHELL -c "tar czf $OUT/$tarball.tar.gz -C $t -s '/^\./$tarball/HS'  ."
