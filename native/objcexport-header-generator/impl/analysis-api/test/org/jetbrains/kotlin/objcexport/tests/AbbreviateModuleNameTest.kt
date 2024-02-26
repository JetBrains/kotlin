/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.objcexport.abbreviateModuleName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AbbreviateModuleNameTest {
    @Test
    fun `test - empty string`() {
        assertEquals("", abbreviateModuleName(""))
    }

    @Test
    fun `test - simple name`() {
        assertEquals("Foo", abbreviateModuleName("Foo"))
    }

    @Test
    fun `test - simple lowercase name`() {
        assertEquals("Foo", abbreviateModuleName("foo"))
    }

    @Test
    fun `test - longer module name`() {
        assertEquals("LMN", abbreviateModuleName("LongModuleName"))
    }

    @Test
    fun `test - longer module name - starting lowercase`() {
        assertEquals("LMN", abbreviateModuleName("longModuleName"))
    }

    @Test
    fun `test - very long module name`() {
        assertEquals("TIAVLMN", abbreviateModuleName("thisIsAVeryLongModuleName"))
    }
}