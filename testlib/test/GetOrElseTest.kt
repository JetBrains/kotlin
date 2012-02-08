package test.standard

import std.*
import stdhack.test.*

class GetOrElseTest() : TestSupport() {
    val v1: String? = "hello"
    val v2: String? = null

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

    /** TODO not supported yet?
    
    fun testLazyDefaultValue() {
        var counter = 0

        assertEquals("hello", v1?: { counter++; "bar"})
        assertEquals(counter, 0, "counter should not be incremented yet")

        assertEquals("bar", v2?: { counter++; "bar"})
        assertEquals(counter, 1, "counter should be incremented in the default function")
    }
    */

    fun testLazyDefaultValueUsingMethod() {
        var counter = 0

        assertEquals("hello", v1.getOrElse{ counter++; "bar"})
        assertEquals(counter, 0, "counter should not be incremented yet")

        assertEquals("bar", v2.getOrElse{ counter++; "bar"})
        assertEquals(counter, 1, "counter should be incremented in the default function")
    }
}
