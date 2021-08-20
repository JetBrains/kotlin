// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR

// WITH_RUNTIME

// MODULE: lib
// FILE: lib.kt

package a

import kotlinx.serialization.*

@Serializable
open class OpenBody {
    var optional: String? = "foo"
}

@Serializable
abstract class AbstractConstructor(var optional: String = "foo")


// MODULE: app(lib)
// FILE: app.kt

package test

import a.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.assertEquals

@Serializable
class Test1: OpenBody()

@Serializable
class Test2: AbstractConstructor()

fun test1() {
    val string = Json.encodeToString(Test1.serializer(), Test1())
    assertEquals("{}", string)
    val reconstructed = Json.decodeFromString(Test1.serializer(), string)
    assertEquals("foo", reconstructed.optional)
}

fun test2() {
    val string = Json.encodeToString(Test2.serializer(), Test2())
    assertEquals("{}", string)
    val reconstructed = Json.decodeFromString(Test2.serializer(), string)
    assertEquals("foo", reconstructed.optional)
}

fun box(): String {
    test1()
    test2()
    return "OK"
}