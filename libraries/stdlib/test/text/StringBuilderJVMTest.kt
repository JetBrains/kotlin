@file:kotlin.jvm.JvmVersion
package test.text

import kotlin.test.*
import org.junit.Test

class StringBuilderJVMTest() {

    @Test fun stringBuildWithInitialCapacity() {
        val s = buildString(123) {
            assertEquals(123, capacity())
        }
    }

    @Test fun getAndSetChar() {
        val sb = StringBuilder("abc")
        sb[1] = 'z'

        assertEquals("azc", sb.toString())
        assertEquals('c', sb[2])
    }
}
