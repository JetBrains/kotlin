# Platform libraries

See [the official documentation](https://kotlinlang.org/docs/native-platform-libs.html). 

### Building

By default, building platform libraries is performed by the snapshot compiler (i.e. freshly built on the current branch).

Setting `kotlin.native.platformLibs.bootstrap=true` Gradle property, will use the bootstrap compiler instead.
This can improve distribution build time when Gradle build caches are used.

Note: building caches for the libraries is always performed by the snapshot compiler.
