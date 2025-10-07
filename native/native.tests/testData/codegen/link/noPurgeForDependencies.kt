// MODULE: lib
// FILE: lib.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.convert
import platform.posix.*

fun foo(): String {
    val size: size_t = 17.convert<size_t>()
    val e = fabs(1.toDouble())
    return "$size; $e"
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    assertEquals("17; 1.0", foo())
    return "OK"
}