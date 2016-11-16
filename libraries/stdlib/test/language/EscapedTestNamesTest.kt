@file:kotlin.jvm.JvmVersion // TODO: Can't run in JS: spaces in function name KT-4160
package test.language

import org.junit.Test
import kotlin.test.*

class EscapedTestNamesTest {

    @Test fun `strings equal`() {
        val actual = "abc"
        assertEquals("abc", actual)
    }

    @Test fun `numbers equal`() {
        val actual = 5
        assertEquals(5, actual)
    }
}