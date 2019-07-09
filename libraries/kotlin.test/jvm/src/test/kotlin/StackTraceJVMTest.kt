/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.tests

import org.junit.Test
import kotlin.test.*

// NOTE: These tests verify line numbers of stack frames, so they might be quite fragile to reordering etc

class StackTraceJVMTest {

    @Test
    fun testCurrentStackTrace() {
/* <-- line number */ val topFrame = currentStackTrace()[0]
        assertEquals("StackTraceJVMTest.kt", topFrame.fileName)
        assertEquals(17, topFrame.lineNumber)
    }

    @Test
    fun testToDo() {
        todo {
            fail("Shouldn't pass here")
        }
    }
}