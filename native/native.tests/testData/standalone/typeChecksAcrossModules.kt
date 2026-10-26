// TARGET_BACKEND: NATIVE
// OUTPUT_DATA_FILE: typeChecksAcrossModules.out

// The test is about the fast type checks and interface dispatch in optimized binaries linked
// against static caches: the hierarchy the checks are built on is assembled from the caches'
// slices by the final link. Other configurations keep the program correct but do not exercise it.

// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=NO
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST

// MODULE: lib
// FILE: lib.kt
package lib

interface Marker
open class Base
open class Middle : Base(), Marker

// MODULE: impl(lib)
// FILE: impl.kt
package impl

class Leaf : lib.Middle()

// MODULE: main(lib, impl)
// FILE: main.kt
import lib.*

class MainLeaf : Middle()

fun main() {
    val leaf: Any = impl.Leaf()
    val middle: Any = Middle()
    val own: Any = MainLeaf()

    // Checks in both directions: the cached class checked against the main module's hierarchy
    // and the main module's class checked against the cached one.
    check(leaf is Base && leaf is Middle && leaf is Marker)
    check(own is Base && own is Middle && own is Marker)
    check(middle !is impl.Leaf && middle !is MainLeaf)
    check(leaf !is MainLeaf)

    println("OK")
}
