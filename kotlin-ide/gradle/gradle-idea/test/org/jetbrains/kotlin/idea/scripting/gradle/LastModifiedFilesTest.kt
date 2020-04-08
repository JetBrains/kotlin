/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.junit.Test
import kotlin.test.assertEquals

class LastModifiedFilesTest {
    val files = LastModifiedFiles()

    @Test
    fun testSimple0() {
        assertEquals(Long.MIN_VALUE, files.lastModifiedTimeStampExcept("x"))
    }

    @Test
    fun testSimple1() {
        files.fileChanged(1, "x")
        assertEquals(Long.MIN_VALUE, files.lastModifiedTimeStampExcept("x"))
        assertEquals(1, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSimple2() {
        files.fileChanged(1, "x")
        files.fileChanged(2, "y")
        assertEquals(2, files.lastModifiedTimeStampExcept("x"))
        assertEquals(1, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSimple3() {
        files.fileChanged(1, "x")
        files.fileChanged(2, "y")
        files.fileChanged(3, "z")
        assertEquals(3, files.lastModifiedTimeStampExcept("x"))
        assertEquals(3, files.lastModifiedTimeStampExcept("y"))
        assertEquals(2, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testSame2() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        assertEquals(1, files.lastModifiedTimeStampExcept("x"))
        assertEquals(1, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSame3() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        files.fileChanged(2, "z")
        assertEquals(2, files.lastModifiedTimeStampExcept("x"))
        assertEquals(2, files.lastModifiedTimeStampExcept("y"))
        assertEquals(1, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testSame4() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        files.fileChanged(2, "z")
        files.fileChanged(3, "a")
        files.fileChanged(3, "b")
        assertEquals(3, files.lastModifiedTimeStampExcept("x"))
        assertEquals(3, files.lastModifiedTimeStampExcept("y"))
        assertEquals(3, files.lastModifiedTimeStampExcept("z"))
        assertEquals(3, files.lastModifiedTimeStampExcept("a"))
        assertEquals(3, files.lastModifiedTimeStampExcept("b"))
    }
}