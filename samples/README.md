# Samples

This directory contains a set of samples demonstrating how one can work with Kotlin/Native. The samples can be
built using Gradle build tool. See `README.md` in sample directories to learn more about specific samples and
the building process.

  * `androidNativeActivity` - Android Native Activity rendering 3D graphics using OpenGLES
  * `calculator` - iOS Swift application, using Kotlin/Native code compiled into the framework
  * `cocoapods` - A Kotlin/Native application using the `AFNetworking` library from CocoaPods.
  * `csvparser` - simple CSV file parser and analyzer
  * `echoServer` - TCP/IP echo server
  * `gitchurn` - program interoperating with `libgit2` for GIT repository analysis
  * `gtk` - GTK2 interoperability example
  * `html5Canvas` - WebAssembly example
  * `libcurl` - using of FTP/HTTP/HTTPS client library `libcurl`
  * `nonBlockingEchoServer` - multi-client TCP/IP echo server using co-routines
  * `objc` - AppKit Objective-C interoperability example for macOS
  * `opengl` - OpenGL/GLUT teapot example
  * `python_extension` - Python extension written in Kotlin/Native
  * `tensorflow` - simple client for TensorFlow Machine Intelligence library
  * `tetris` - Tetris game implementation (using SDL2 for rendering)
  * `uikit` - UIKit Objective-C interoperability example for iOS
  * `videoplayer` - SDL and FFMPEG-based video and audio player
  * `win32` - trivial Win32 GUI application
  * `workers` - example of using workers API


**Note**: If the samples are built from a source tree (not from a distribution archive) the compiler built from
the sources is used. So you must build the compiler and the necessary platform libraries by running
`./gradlew bundle` from the Kotlin/Native root directory before building samples (see
[README.md](https://github.com/JetBrains/kotlin-native/blob/master/README.md) for details).

Alternatively you may remove a line `org.jetbrains.kotlin.native.home=<...>` from all `gradle.properties` files.
In this case the Gradle plugin downloads and uses a default compiler for this plugin version.

One may also build all the samples with one command. To build them using Gradle run:

    ./gradlew buildAllSamples
