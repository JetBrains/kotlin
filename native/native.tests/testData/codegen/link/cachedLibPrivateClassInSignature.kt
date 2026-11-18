// NATIVE_STANDALONE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_PER_FILE_EVERYWHERE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib1
// FILE: lib1.kt

private class Hidden {
    override fun toString() = "FAIL"
}

// An exported function with a non-exported class in its signature.
@Suppress("EXPOSED_PARAMETER_TYPE")
fun describe(hidden: Hidden?): String = hidden?.toString() ?: "OK"

// MODULE: lib2(lib1)
// FILE: lib2.kt

// The cache of this library references a function, whose signature mentions another library's private class.
fun describeNothing(): String = describe(null)

// MODULE: main(lib1, lib2)
// FILE: main.kt

fun box(): String = describeNothing()
