# Samples

This directory contains a set of samples demonstrating how one can work with Kotlin/Native. The samples can be
built using either command line tools (via `build.sh` script presented in each sample directory) or using a gradle build.
See `README.md` in sample directories to learn more about specific samples and the building process.

**Note**: If the samples are built from a source tree (not from a distribution archive) the compiler and the gradle
plugin built from the sources are used. So one must build the compiler by running `./gradlew cross_dist` from the 
Kotlin/Native root directory before building samples (see
[README.md](https://github.com/JetBrains/kotlin-native/blob/master/README.md) for details).

One may also build all the samples with one command. To build them using the command line tools run:

    ./build.sh
    
To build all the samples using the gradle build:

    ./gradlew build
    
One also may launch the command line build via a gradle task `buildSh` (equivalent of `./build.sh` executing):

    ./gradlew buildSh
