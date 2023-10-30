@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import library.*
import kotlin.test.assertEquals

class Impl : WithClassProperty() {
    companion object {
        fun stringProperty(): String? = "42"
    }
}

fun main() {
    assertEquals("42", Impl.stringProperty())
}