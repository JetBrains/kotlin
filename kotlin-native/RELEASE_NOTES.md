# Kotlin/Native backend, Beta version #

## Introduction ##

 _Kotlin/Native_ is an LLVM backend for the Kotlin compiler.
It consists of a machine code generation facility using the LLVM toolchain
and a native runtime implementation.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS or embedded targets),
or where the developer needs to produce a reasonably-sized self-contained binary
that doesn't require an additional execution runtime.

## Supported platforms ##

The _Kotlin/Native_ compiler produces mostly portable (modulo pointer size and target
triplet) LLVM bitcode, and as such can easily support any platform, as long as there's an LLVM
code generator for the platform.
 However, as actually producing native code requires a platform linker and some
basic runtime shipped along with the translator, we only support a subset of all possible
target platforms. Currently _Kotlin/Native_ is being shipped and tested with support for
the following platforms:

 * Mac OS X 10.11 and later (x86-64), host and target (`-target macos_x64`, default on macOS hosts)
 * Ubuntu Linux x86-64 (14.04, 16.04 and later), other Linux flavours may work as well, host and target
   (`-target linux_x64`, default on Linux hosts, hosted on Linux, Windows and macOS).
 * Microsoft Windows x86-64 (tested on Windows 7 and Windows 10), host and target (`-target mingw_x64`,
   default on Windows hosts).
 * Microsoft Windows x86-32 cross-compiled target (`-target mingw_x86`), hosted on Windows.
 * Apple iOS (armv7 and arm64 devices, x86 simulator), cross-compiled target
   (`-target ios_arm32|ios_arm64|ios_x64`), hosted on macOS.
 * Apple tvOS (arm64 devices, x86 simulator), cross-compiled target
    (`-target tvos_arm64|tvos_x64`), hosted on macOS.
 * Apple watchOS (arm32/arm64 devices, x86 simulator), cross-compiled target
     (`-target watchos_arm32|watchos_arm64|watchos_x86|watchos_x64`), hosted on macOS.
 * Linux arm32 hardfp, Raspberry Pi, cross-compiled target (`-target raspberrypi`), hosted on Linux, Windows and macOS
 * Linux MIPS big endian, cross-compiled target (`-target mips`), hosted on Linux.
 * Linux MIPS little endian, cross-compiled target (`-target mipsel`), hosted on Linux.
 * Android x86 (32 and 64 bit), arm32 and arm64 (`-target android_x86|android_x64|android_arm32|android_arm64`) targets,
   hosted on Linux, macOS and Windows.
 * WebAssembly (`-target wasm32`) target, hosted on Linux, Windows or macOS. Webassembly support is experimental
   and could be discontinued in further releases.
 * Experimental support for Zephyr RTOS (`-target zephyr_stm32f4_disco`) is available on macOS, Linux
   and Windows hosts. "Experimental" here also means that support for this target might be broken at the moment,
   and UX might be disappointing.

 ## Compatibility and features ##

To run _Kotlin/Native_ compiler JDK 8 or later  (JDK) for the host platform has to be installed.
Produced programs are fully self-sufficient and do not need JVM or other runtime.

On macOS it also requires Xcode 11.0 or newer to be installed.

The language and library version supported by this release match Kotlin 1.5.
However, there are certain limitations, see section [Known Limitations](#limitations).

 Currently _Kotlin/Native_ uses reference counting based memory management scheme with a cycle
collection algorithm. Multiple threads could be used, but objects must be explicitly transferred
between threads, and same object couldn't be accessed by two threads concurrently unless it is frozen.
See the relevant [documentation](https://kotlinlang.org/docs/reference/native/concurrency.html).
We are going to lift these multithreading restrictions, which involves implementing a new memory manager.
More details are available in
["Kotlin/Native Memory Management Roadmap"](https://blog.jetbrains.com/kotlin/2020/07/kotlin-native-memory-management-roadmap/).

_Kotlin/Native_ provides efficient bidirectional interoperability with C and Objective-C.
See the [samples](https://github.com/JetBrains/kotlin-native/tree/master/samples)
and the [tutorials](https://kotlinlang.org/docs/tutorials/).

  ## Getting Started ##
  
The most complete experience with Kotlin/Native can be achieved by using
[Gradle](https://kotlinlang.org/docs/tutorials/native/using-gradle.html),
[IntelliJ IDEA](https://kotlinlang.org/docs/tutorials/native/using-intellij-idea.html) or
[Android Studio with KMM plugin](https://kotlinlang.org/docs/mobile/create-first-app.html) if you target iOS.

If you are interested in using Kotlin/Native for iOS, then
[Kotlin Multiplatform Mobile portal](https://kotlinlang.org/lp/mobile/) might also be useful for you.
 
Command line compiler is also
[available](https://kotlinlang.org/docs/tutorials/native/using-command-line-compiler.html).

More information can be found in the overviews of
[Kotlin/Native](https://kotlinlang.org/docs/reference/native-overview.html)
and [Kotlin Multiplatform](https://kotlinlang.org/docs/reference/multiplatform.html).

 ## <a name="limitations"></a>Known limitations ##

 ### Performance ###

 *** DO NOT USE THIS PREVIEW RELEASE FOR ANY PERFORMANCE ANALYSIS ***

 This beta version of _Kotlin/Native_ technology is not yet tuned
for benchmarking and competitive analysis of any kind.

### Standard Library ###

  The standard library in _Kotlin/Native_ is known match common standard library in other Kotlin variants.
 Note, that standard Java APIs, such as `java.math.BigDecimal` or `java.io`
is not available in current _Kotlin_ standard library, but using C interoperability, one could
call similar APIs from the POSIX library, see this [`sample`](https://github.com/JetBrains/kotlin-native/blob/master/samples/csvparser).
  Also Kotlin/Native standard library contains certain native-specific extensions, mostly around
memory management and concurrency.

### Reflection ###

Full reflection is not implemented, but class can be referenced and its name could be retrieved.
Notice that property delegation (including lazy properties) *does* work.

### Microsoft Windows support ###

 Only 64-bit Windows is currently supported as compilation host, both 32-bit and 64-bit Windows could
be targets.

### Debugging ###

 _Kotlin/Native_ supports  source-level debugging on produced executables with `lldb` debugger.
 Produce your binary with debugging information by specifying `-g` _Kotlin/Native_ compiler switch.
See [`DEBUGGING.md`](https://github.com/JetBrains/kotlin-native/blob/master/DEBUGGING.md) for further information.
