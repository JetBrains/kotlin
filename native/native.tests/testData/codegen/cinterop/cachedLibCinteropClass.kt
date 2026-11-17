// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_PER_FILE_EVERYWHERE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: cinterop
// FILE: cinterop.def
---
struct Point {
    int x;
    int y;
};

// MODULE: lib(cinterop)
// FILE: lib.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("DEPRECATION_ERROR")

import cinterop.*
import kotlinx.cinterop.*

// The object is referenced from the cached library, but its class belongs to the cinterop library.
fun getPointType(): Any = Point

// MODULE: main(cinterop, lib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("DEPRECATION_ERROR")

import cinterop.*
import kotlinx.cinterop.*
import kotlin.test.assertEquals

// A virtual call: the class is final, so a call on a receiver of that very type would be a direct one.
private fun hashOf(obj: Any): Int = obj.hashCode()

fun box(): String {
    val type = getPointType()
    assertEquals(type.hashCode(), hashOf(type as Point.Companion))
    return "OK"
}
