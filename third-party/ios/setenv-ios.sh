#!/usr/bin/env bash

# Use as 'source setenv-ios.sh device|simulator'.

# ====================================================================
# Sets the cross compile environment for Xcode/iOS
# Based upon OpenSSL's setenv-ios.sh  (by TH, JW, and SM).
#
# Crypto++ Library is copyrighted as a compilation and (as of version 5.6.2)
# licensed under the Boost Software License 1.0, while the individual files
# in the compilation are all public domain.
#
# See http://www.cryptopp.com/wiki/iOS_(Command_Line) for more details
# ====================================================================

#########################################
#####       Clear old options       #####
#########################################

unset IS_CROSS_COMPILE

unset IS_IOS
unset IS_ANDROID
unset IS_ARM_EMBEDDED

unset IOS_ARCH
unset IOS_FLAGS
unset IOS_SYSROOT

#########################################
#####   User configurable options   #####
#########################################

# Define SETENV_VERBOSE=1 to print the configuration, including exported variables.
SETENV_VERBOSE=1

# For various SDKs, see https://groups.google.com/d/msg/cryptopp-users/8Z0qfwAjSbA/nKYbhTNBBgAJ

########################################
#####         Command line         #####
########################################

for ARG in "$@"
do
  CL=$(echo $ARG | tr '[A-Z]' '[a-z]')

  # i386 (simulator)
  if [ "$CL" == "i386" ]; then
    IOS_ARCH=i386
  fi

  # x86_64 (simulator)
  if [ "$CL" == "x86_64" ]; then
    IOS_ARCH=x86_64
  fi

  # ARMv5
  if [ "$CL" == "armv5" ]; then
    IOS_ARCH=armv5
  fi

  # ARMv6
  if [ "$CL" == "armv6" ]; then
    IOS_ARCH=armv6
  fi

  # ARMv7
  if [ "$CL" == "armv7" ]; then
    IOS_ARCH=armv7
  fi

  # ARMv7s
  if [ "$CL" == "armv7s" ]; then
    IOS_ARCH=armv7s
  fi

  # ARM64
  if [ "$CL" == "arm64" ]; then
    IOS_ARCH=arm64
  fi

  # iPhone
  if [ "$CL" == "iphone" ] || [ "$CL" == "iphoneos" ]; then
    APPLE_SDK=iPhoneOS
  fi

  # iPhone Simulator
  if [ "$CL" == "simulator" ] || [ "$CL" == "iphonesimulator" ]; then
    APPLE_SDK=iPhoneSimulator
  fi

  # Watch
  if [ "$CL" == "watch" ] || [ "$CL" == "watchos" ] || [ "$CL" == "applewatch" ]; then
    APPLE_SDK=WatchOS
  fi

  # Watch Simulator
  if [ "$CL" == "watchsimulator" ]; then
    APPLE_SDK=WatchSimulator
  fi

  # Apple TV
  if [ "$CL" == "tv" ] || [ "$CL" == "appletv" ] || [ "$CL" == "appletvos" ]; then
    APPLE_SDK=AppleTVOS
  fi

  # Apple TV Simulator
  if [ "$CL" == "tvsimulator" ] || [ "$CL" == "appletvsimulator" ]; then
    APPLE_SDK=AppleTVSimulator
  fi

done

# Defaults if not set
if [ -z "$APPLE_SDK" ]; then
	APPLE_SDK=iPhoneOS
fi

if [ -z "$IOS_ARCH" ]; then
	if [ "$APPLE_SDK" == "iPhoneOS" ]; then
		IOS_ARCH=armv7
	elif [ "$APPLE_SDK" == "iPhoneSimulator" ]; then
		IOS_ARCH=i386
	elif [ "$APPLE_SDK" == "AppleTVOS" ]; then
		IOS_ARCH=arm64
	elif [ "$APPLE_SDK" == "WatchOS" ]; then
		IOS_ARCH=armv7
	fi

	# TODO: fill in missing simulator architectures
fi

# Allow a user override? I think we should be doing this. The use case is:
# move /Applications/Xcode somewhere else for a side-by-side installation.
# These sorts of tricks are a required procedure on Apple's gear:
# http://stackoverflow.com/questions/11651773/install-simulator-sdk-4-3-to-xcode-4-4-on-mountain-lion
if [ -z "$XCODE_DEVELOPER" ]; then
  XCODE_DEVELOPER=$(xcode-select -print-path 2>/dev/null)
fi

if [ ! -d "$XCODE_DEVELOPER" ]; then
  echo "ERROR: unable to find XCODE_DEVELOPER directory."
  [ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

# Default toolchain location
XCODE_TOOLCHAIN="$XCODE_DEVELOPER/usr/bin"

if [ ! -d "$XCODE_TOOLCHAIN" ]; then
  echo "ERROR: unable to find XCODE_TOOLCHAIN directory."
  [ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

# XCODE_DEVELOPER_TOP is the top of the development tools tree
XCODE_DEVELOPER_TOP="$XCODE_DEVELOPER/Platforms/$APPLE_SDK.platform/Developer"

if [ ! -d "$XCODE_DEVELOPER_TOP" ]; then
  echo "ERROR: unable to find XCODE_DEVELOPER_TOP directory."
  [ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

# IOS_TOOLCHAIN is the location of the actual compiler tools.
if [ -d "$XCODE_DEVELOPER/Toolchains/XcodeDefault.xctoolchain/usr/bin/" ]; then
  IOS_TOOLCHAIN="$XCODE_DEVELOPER/Toolchains/XcodeDefault.xctoolchain/usr/bin/"
elif [ -d "$XCODE_DEVELOPER_TOP/usr/bin/" ]; then
  IOS_TOOLCHAIN="$XCODE_DEVELOPER_TOP/usr/bin/"
fi

if [ -z "$IOS_TOOLCHAIN" ] || [ ! -d "$IOS_TOOLCHAIN" ]; then
  echo "ERROR: unable to find Xcode cross-compiler tools."
  [ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

#
# XCODE_SDK is the SDK name/version being used - adjust the list as appropriate.
# For example, remove 4.3, 6.2, and 6.1 if they are not installed. We go back to
# the 1.0 SDKs because Apple WatchOS uses low numbers, like 2.0 and 2.1.
unset XCODE_SDK
for i in $(seq -f "%.1f" 20.0 -0.1 1.0)
do
	if [ -d "$XCODE_DEVELOPER/Platforms/$APPLE_SDK.platform/Developer/SDKs/$APPLE_SDK$i.sdk" ]; then
    	XCODE_SDK="$APPLE_SDK$i.sdk"
      	break
	fi
done

# Error checking
if [ -z "$XCODE_SDK" ]; then
    echo "ERROR: unable to find a SDK."
    [ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

# Simulator fixup. LD fails to link dylib.
if [ "$APPLE_SDK" == "iPhoneSimulator" ] && [ "$IOS_ARCH" == "i386" ]; then
  IOS_FLAGS=-miphoneos-version-min=5
fi

# ARMv7s fixup. Xcode 4/iOS 6
if [ "$IOS_ARCH" == "armv7s" ]; then
  IOS_FLAGS=-miphoneos-version-min=6
fi

# ARM64 fixup. Xcode 5/iOS 7
if [ "$IOS_ARCH" == "arm64" ]; then
  IOS_FLAGS=-miphoneos-version-min=7
fi

# ARM64 Simulator fixup. Under Xcode 6/iOS 8, it uses x86_64 and not i386
if [ "$IOS_ARCH" == "x86_64" ]; then
  IOS_FLAGS=-miphoneos-version-min=8
fi

# Simulator uses i386 or x86_64, Device uses ARMv5, ARMv6, ARMv7, or ARMv7s
#
# Apple deprecated ARMv5 at iOS 4.0, and ARMv6 at iOS 5.0
# http://stackoverflow.com/questions/7488657/how-to-build-for-armv6-and-armv7-architectures-with-ios-5

echo "Configuring for $APPLE_SDK ($IOS_ARCH)"

# Used by the GNUmakefile-cross
export IS_IOS=1
export IOS_ARCH
export IOS_FLAGS
export IOS_SYSROOT="$XCODE_DEVELOPER_TOP/SDKs/$XCODE_SDK"

#######################################
#####          Verbose           #####
#######################################

if [ "$SETENV_VERBOSE" == "1" ]; then

  echo "XCODE_SDK:" $XCODE_SDK
  echo "XCODE_DEVELOPER: $XCODE_DEVELOPER"
  echo "XCODE_TOOLCHAIN: $XCODE_TOOLCHAIN"
  echo "XCODE_DEVELOPER_TOP: $XCODE_DEVELOPER_TOP"
  echo "IOS_ARCH: $IOS_ARCH"
  echo "IOS_TOOLCHAIN: $IOS_TOOLCHAIN"
  echo "IOS_FLAGS: $IOS_FLAGS"
  echo "IOS_SYSROOT: $IOS_SYSROOT"
fi

########################################
#####     Path with Toolchains     #####
########################################

# Only modify/export PATH if IOS_TOOLCHAIN good
if [ ! -z "$IOS_TOOLCHAIN" ] && [ ! -z "$XCODE_TOOLCHAIN" ]; then

	# And only modify PATH if IOS_TOOLCHAIN is not present
	TOOL_PATH="$IOS_TOOLCHAIN:$XCODE_TOOLCHAIN"
	LEN=${#TOOL_PATH}
	SUBSTR=${PATH:0:$LEN}
	if [ "$SUBSTR" != "$TOOL_PATH" ]; then
		export PATH="$TOOL_PATH":"$PATH"
	fi
else
	echo "ERROR: unable to set new PATH."
	[ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

########################################
#####        Tool Test Time        #####
########################################

# Test for various tools needed during cross compilation.
# FOUND_ALL starts high, and pushes low on failure
FOUND_ALL=1

# Apple's embedded g++ cannot compile integer.cpp
TOOLS=(clang clang++ ar ranlib libtool ld)
for tool in ${TOOLS[@]}
do
	if [ ! -e "$IOS_TOOLCHAIN/$tool" ] && [ ! -e "$XCODE_TOOLCHAIN/$tool" ]; then
		echo "ERROR: unable to find $tool at IOS_TOOLCHAIN or XCODE_TOOLCHAIN"
		FOUND_ALL=0
	fi
done

if [ "$FOUND_ALL" -eq "0" ]; then
	[ "$0" = "$BASH_SOURCE" ] && exit 1 || return 1
fi

echo
echo "*******************************************************************************"
echo "It looks the the environment is set correctly. Your next step is"
echo "build the library with 'make -f GNUmakefile-cross'"
echo "*******************************************************************************"
echo

[ "$0" = "$BASH_SOURCE" ] && exit 0 || return 0
