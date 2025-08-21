/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.objcexport.normalizeAndAbbreviateModuleName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AbbreviateModuleNameTest {
    @Test
    fun `test - empty string`() {
        assertEquals("", normalizeAndAbbreviateModuleName(""))
    }

    @Test
    fun `test - simple name`() {
        assertEquals("Foo", normalizeAndAbbreviateModuleName("Foo"))
    }

    @Test
    fun `test - simple lowercase name`() {
        assertEquals("Foo", normalizeAndAbbreviateModuleName("foo"))
    }

    @Test
    fun `test - longer module name`() {
        assertEquals("LMN", normalizeAndAbbreviateModuleName("LongModuleName"))
    }

    @Test
    fun `test - longer module name - starting lowercase`() {
        assertEquals("LMN", normalizeAndAbbreviateModuleName("longModuleName"))
    }

    @Test
    fun `test - very long module name`() {
        assertEquals("TIAVLMN", normalizeAndAbbreviateModuleName("thisIsAVeryLongModuleName"))
    }

    @Test
    fun `test - gradle serialization-core-iosarm64`() {
        val actual = normalizeAndAbbreviateModuleName("Gradle: org.jetbrains.kotlinx:kotlinx-serialization-core-iosarm64:1.8.1")
        assertEquals("Gradle__org_jetbrains_kotlinx_kotlinx_serialization_core_iosarm64_1_8_1", actual)
    }
}