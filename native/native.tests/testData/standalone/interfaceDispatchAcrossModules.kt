// TARGET_BACKEND: NATIVE
// OUTPUT_DATA_FILE: interfaceDispatchAcrossModules.out

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

interface Greeter {
    fun greet(): String
}

class CachedGreeter : Greeter {
    override fun greet() = "cached"
}

// MODULE: impl(lib)
// FILE: impl.kt
package impl

class SecondGreeter : lib.Greeter {
    override fun greet() = "second"
}

// MODULE: main(lib, impl)
// FILE: main.kt
import lib.Greeter

class MainGreeter : Greeter {
    override fun greet() = "main"
}

fun main() {
    // The interface, one implementation and the call site are in three different modules,
    // so the itable slot read by the call can only be assigned by the final link.
    val greeters: List<Greeter> = listOf(lib.CachedGreeter(), impl.SecondGreeter(), MainGreeter())
    check(greeters.joinToString(" ") { it.greet() } == "cached second main")
    check(greeters.all { it is Greeter })

    println("OK")
}
