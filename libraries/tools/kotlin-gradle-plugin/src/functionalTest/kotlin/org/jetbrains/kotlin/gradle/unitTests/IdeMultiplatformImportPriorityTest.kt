/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.Priority
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IdeMultiplatformImportPriorityTest {

    @Test
    fun `test - equals and hashCode`() {
        assertEquals(Priority(10), Priority(10))
        assertEquals(Priority(10).hashCode(), Priority(10).hashCode())

        assertNotEquals(Priority(10), Priority(11))
        assertNotEquals(Priority(10).hashCode(), Priority(11).hashCode())
    }

    @Test
    fun `test - compareTo`() {
        assertTrue(Priority(10) > Priority(9))
        assertTrue(Priority(9) < Priority(10))
        assertTrue(Priority(9) < Priority(10))
        assertTrue(Priority(10) <= Priority(10))
        assertTrue(Priority(10) >= Priority(10))
    }

    @Test
    fun `test - predefined values`() {
        val predefinedValues = listOf(
            Priority.low,
            Priority.normal,
            Priority.high,
            Priority.veryHigh
        )

        assertEquals(predefinedValues, predefinedValues.sorted())
        assertEquals(predefinedValues.reversed(), predefinedValues.sortedDescending())

        predefinedValues.zipWithNext { lower, higher ->
            assertNotEquals(lower, higher)
            assertTrue(lower < higher)
        }
    }
}