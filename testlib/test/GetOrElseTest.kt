package test.standard

import std.*
import stdhack.test.*

class GetOrElseTest() : TestSupport() {
    val v1: String? = "hello"
    val v2: String? = null
    var counter = 0

    fun testDefaultValue() {
        assertEquals("hello", v1?: "bar")

        expect("hello") {
            v1?: "bar"
        }
    }

    fun testDefaultValueOnNull() {
        assertEquals("bar", v2?: "bar")

        expect("bar") {
            v2?: "bar"
        }
    }

    fun calculateBar(): String {
        counter++
        return "bar"
    }

    fun testLazyDefaultValue() {
        counter = 0

        assertEquals("hello", v1?: calculateBar())
        assertEquals(counter, 0, "counter should not be incremented yet")

        assertEquals("bar", v2?: calculateBar())
        assertEquals(counter, 1, "counter should be incremented in the default function")
    }
}
