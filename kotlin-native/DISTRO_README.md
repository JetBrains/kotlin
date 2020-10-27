# Kotlin/Native  #

_Kotlin/Native_ is a LLVM backend for the Kotlin compiler, runtime
implementation and native code generation facility using LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS, embedded targets),
or where developer is willing to produce reasonably-sized self-contained program
without need to ship an additional execution runtime.

 _Kotlin/Native_ could be used either as standalone compiler toolchain or as Gradle
plugin. See [documentation](https://kotlinlang.org/docs/reference/native/gradle_plugin.html)
for more details on how to use this plugin.

Compile your programs like that:

    export PATH=kotlin-native-<platform>-<version>/bin:$PATH
	kotlinc hello.kt -o hello

For an optimized compilation use -opt:

	kotlinc hello.kt -o hello -opt

To generate interoperability stubs create library definition file
(take a look on [Tetris sample](samples/tetris))
and run `cinterop` tool like this:

    cinterop -def lib.def

See [C interop documentation](https://kotlinlang.org/docs/reference/native/c_interop.html)
for more information on how to use C libraries from _Kotlin/Native_.

See [`RELEASE_NOTES.md`](https://github.com/JetBrains/kotlin-native/blob/master/RELEASE_NOTES.md) for information on supported platforms and current limitations.
