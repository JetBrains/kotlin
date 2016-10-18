@file:kotlin.jvm.JvmVersion
package test.text

import kotlin.test.*
import org.junit.Test as test

class StringBuilderJVMTest() {

    @test fun stringBuildWithInitialCapacity() {
        val s = buildString(123) {
            assertEquals(123, capacity())
        }
    }

    @test fun getAndSetChar() {
        val sb = StringBuilder("abc")
        sb[1] = 'z'

        assertEquals("azc", sb.toString())
        assertEquals('c', sb[2])
    }
}
