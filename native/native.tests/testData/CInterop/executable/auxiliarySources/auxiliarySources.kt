@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import auxiliarySources.*
import kotlin.test.*
import kotlinx.cinterop.*

fun main() {
    assertEquals(name()!!.toKString(), "Hello from C++")
}
