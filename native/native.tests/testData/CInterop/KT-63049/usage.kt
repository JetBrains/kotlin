@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import library.*
import kotlin.test.assertEquals

class Impl : WithClassProperty() {
    companion object : WithClassPropertyMeta() {
        fun stringProperty(): String? = "42"
    }
}

fun main() {
    assertEquals("42", Impl.stringProperty())
}
