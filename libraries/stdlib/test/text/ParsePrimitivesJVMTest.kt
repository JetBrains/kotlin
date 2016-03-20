@file:kotlin.jvm.JvmVersion
package test.text

import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.junit.Test as test

class ParsePrimitivesJVMTest {

    @test fun toBoolean() {
        assertEquals(true, "true".toBoolean())
        assertEquals(true, "True".toBoolean())
        assertEquals(false, "false".toBoolean())
        assertEquals(false, "not so true".toBoolean())
    }

    @test fun toByte() {
        assertEquals(77.toByte(), "77".toByte())
        assertFails { "255".toByte() }
    }

    @test fun toShort() {
        assertEquals(77.toShort(), "77".toShort())
    }

    @test fun toInt() {
        assertEquals(77, "77".toInt())
    }

    @test fun toLong() {
        assertEquals(77.toLong(), "77".toLong())
    }
}
