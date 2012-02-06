package test.standard

import std.*
import stdhack.test.*
import stdhack.test.assertEquals

class GetOrElseTest() : TestSupport() {
    val v1: String? = "hello"
    val v2: String? = null

    fun testDefaultValue() {
        /**
        TODO compiler error?

        assertEquals("hello", v1.getOrElse("bar"))
        assertEquals("bar", v2.getOrElse("bar"))

        expect("hello") {
            v1.getOrElse("bar")
        }
        expect("bar") {
            v2.getOrElse("bar")
        }
        */
    }

    fun testLazyDefaultValue() {
        var counter = 0

        /**
        TODO compiler error?
        assertEquals("hello", v1.getOrElse{ counter++; "bar"}
        assertEquals(counter, 0, "counter should not be incremented yet")

        assertEquals("bar", v2.getOrElse{ counter++; "bar"}
        assertEquals(counter, 1, "counter should be incremented in the default function")
        */
    }
}
