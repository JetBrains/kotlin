@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import library.*
import kotlin.test.assertEquals

class Impl : NotFromNSObject() {
    companion object {
        fun stringProperty(): String? = "42"
    }
}

fun main() {
    assertEquals("42", Impl.stringProperty())
}
