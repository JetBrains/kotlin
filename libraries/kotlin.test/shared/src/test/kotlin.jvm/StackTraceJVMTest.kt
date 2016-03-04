package kotlin.test.tests

import org.junit.Test
import kotlin.test.*

// NOTE: These tests verify line numbers of stack frames, so they might be quite fragile to reordering etc

class StackTraceJVMTest {

    @Test
    fun testCurrentStackTrace() {
/* <-- line number */ val topFrame = currentStackTrace()[0]
        assertEquals("StackTraceJVMTest.kt", topFrame.fileName)
        assertEquals(12, topFrame.lineNumber)
    }

    @Test
    fun testToDo() {
        todo {
            fail("Shouldn't pass here")
        }
    }
}