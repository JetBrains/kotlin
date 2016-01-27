package kotlinx.testing.tests

import kotlin.test.*
import org.junit.*

class BasicAssertionsJVMTest {
    @Test
    fun testFailsWith() {
        assertFailsWith(IllegalArgumentException::class) {
            throw IllegalArgumentException()
        }
    }

    @Test(expected = AssertionError::class)
    fun testFailsWithFails() {
        assertFailsWith(IllegalArgumentException::class) {
            throw IllegalStateException()
        }
    }

    @Test
    fun testFailsWithMessage() {
        assertFailsWith<IllegalArgumentException>() {
            throw IllegalArgumentException()
        }
    }
    @Test
    fun testFailsWithClassMessage() {
        @Suppress("UNCHECKED_CAST")
        (assertFailsWith((Class.forName("java.lang.IllegalArgumentException") as Class<Throwable>).kotlin) {
            throw IllegalArgumentException()
        })
    }

    @Test
    fun testToDo() {
        todo {
            fail("Shouldn't pass here")
        }
    }

    @Test
    fun testCurrentStackTrace() {
        assertEquals("BasicAssertionsJVMTest.kt", currentStackTrace()[0].fileName)
    }
}
