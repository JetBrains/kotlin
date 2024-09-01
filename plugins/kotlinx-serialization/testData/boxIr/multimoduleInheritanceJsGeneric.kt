// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_K1: JS_IR
// K1 unsupported because of KT-62215

// MODULE: lib
// FILE: lib.kt

package a

import kotlinx.serialization.*

@Serializable
open class GenericBox<E> {
    var contents: Map<String, E>? = null
}


// MODULE: app(lib)
// FILE: app.kt

package test

import a.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.assertEquals


@Serializable
data class TestData(val field: String)

@Serializable
class TestClass(): GenericBox<TestData>()


fun box(): String {
    val t = TestClass().also { it.contents = mapOf("a" to TestData("data")) }
    val s = Json.encodeToString(t)
    assertEquals("""{"contents":{"a":{"field":"data"}}}""", s)
    val d = Json.decodeFromString<TestClass>(s)
    assertEquals("data", d.contents?.get("a")?.field)
    return "OK"
}
