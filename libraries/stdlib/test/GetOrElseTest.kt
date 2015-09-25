package test.standard

import kotlin.*
import kotlin.test.*
import org.junit.Test as test

class GetOrElseTest {
    val v1: String? = "hello"
    val v2: String? = null
    var counter = 0

    @test fun defaultValue() {
        assertEquals("hello", v1?: "bar")

        expect("hello") {
            v1?: "bar"
        }
    }

    @test fun defaultValueOnNull() {
        assertEquals("bar", v2?: "bar")

        expect("bar") {
            v2?: "bar"
        }
    }

    fun calculateBar(): String {
        counter++
        return "bar"
    }

    @test fun lazyDefaultValue() {
        counter = 0

        assertEquals("hello", v1?: calculateBar())
        assertEquals(counter, 0, "counter should not be incremented yet")

        assertEquals("bar", v2?: calculateBar())
        assertEquals(counter, 1, "counter should be incremented in the default function")
    }
}
