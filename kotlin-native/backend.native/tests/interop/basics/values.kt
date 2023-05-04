@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*
import kotlin.test.*
import cvalues.*

fun main() {
    assertTrue(isNullString(null))
    assertTrue(isNullWString(null))
    assertFalse(isNullString("a"))
    assertFalse(isNullWString("b"))
}
