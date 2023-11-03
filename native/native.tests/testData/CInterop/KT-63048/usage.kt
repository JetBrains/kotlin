@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import library.*
import kotlin.test.assertEquals

@ObjCName("KotlinImplWithCompanionPropertyOverride")
class Impl : WithClassProperty() {
    companion object : WithClassPropertyMeta() {
        override fun stringProperty(): String? = "42"
    }
}

fun main() {
    assertEquals("42", Impl.stringProperty())
}