// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6

// JS vs JVM difference is that we are not able to understand that we have a default value in other module (KT-62523)

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

@Serializable
open class Vehicle {
    var color: String? = null
    var name: String? = null
}


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

@Serializable
open class Car : Vehicle() {
    var maxSpeed: Int = 100
}

fun test1() {
    val string = Json.encodeToString(Test1.serializer(), Test1())
    assertEquals("{\"optional\":\"foo\"}", string)
    val reconstructed = Json.decodeFromString(Test1.serializer(), string)
    assertEquals("foo", reconstructed.optional)
}

fun test2() {
    val string = Json.encodeToString(Test2.serializer(), Test2())
    assertEquals("{\"optional\":\"foo\"}", string)
    val reconstructed = Json.decodeFromString(Test2.serializer(), string)
    assertEquals("foo", reconstructed.optional)
}

fun test3() {
    val json = Json { allowStructuredMapKeys = true; encodeDefaults = true }

    val car = Car()
    car.maxSpeed = 100
    car.name = "ford"
    val s = json.encodeToString(Car.serializer(), car)
    assertEquals("""{"color":null,"name":"ford","maxSpeed":100}""", s)
    val restoredCar = json.decodeFromString(Car.serializer(), s)
    assertEquals(100, restoredCar.maxSpeed)
    assertEquals("ford", restoredCar.name)
    assertEquals(null, restoredCar.color)
}

fun box(): String {
    try {
        test1()
        test2()
        test3()
        return "OK"
    } catch (e: Throwable) {
        e.printStackTrace()
        return e.message!!
    }

}
