// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: toKString.def
---
const char* empty() { return ""; }
const char* foo() { return "foo"; }
const char* kuku() { return "куку"; }
const char* invalid_utf8() { return "\x85\xAF"; }
const char* zero_in_the_middle() { return "before zero\0after zero"; }


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import toKString.*
import kotlinx.cinterop.*
import kotlin.native.*
import kotlin.test.*

fun box(): String {
    assertEquals("", empty()!!.toKStringFromUtf8())
    assertEquals("foo", foo()!!.toKStringFromUtf8())
    assertEquals("куку", kuku()!!.toKStringFromUtf8())
    assertEquals("\uFFFD\uFFFD", invalid_utf8()!!.toKStringFromUtf8())
    assertEquals("before zero", zero_in_the_middle()!!.toKStringFromUtf8())

    return "OK"
}
