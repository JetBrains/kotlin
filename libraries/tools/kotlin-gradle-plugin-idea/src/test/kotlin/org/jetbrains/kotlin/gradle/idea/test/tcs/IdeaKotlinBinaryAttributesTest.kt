/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.tcs

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryAttributes
import kotlin.test.*

class IdeaKotlinBinaryAttributesTest {
    @Test
    fun `test - empty instance is same`() {
        val instance1 = IdeaKotlinBinaryAttributes()
        val instance2 = IdeaKotlinBinaryAttributes()
        assertSame(instance1, instance2)
    }

    @Test
    fun `test - equal instances are same`() {
        val instance1 = IdeaKotlinBinaryAttributes(
            mapOf("a" to "valueA", "b" to "valueB")
        )

        val instance2 = IdeaKotlinBinaryAttributes(
            mapOf("a" to "valueA", "b" to "valueB")
        )

        assertSame(instance1, instance2)
    }

    @Test
    fun `test - instances string values are same`() {
        val instance1 = IdeaKotlinBinaryAttributes(
            mapOf("a" to "valueA", "b" to "valueB", "c" to "xxx")
        )

        val instance2 = IdeaKotlinBinaryAttributes(
            mapOf("a" to "valueA", "b" to "valueB", "c" to "yyy")
        )

        assertNotSame(instance1, instance2)
        assertNotEquals(instance1, instance2)

        instance1.keys.toList().forEachIndexed { index, key ->
            assertSame(key, instance2.keys.toList()[index])
        }

        assertSame(instance1["a"], instance2["a"])
        assertSame(instance1["b"], instance2["b"])
        assertEquals("valueA", instance1["a"])
        assertEquals("valueB", instance1["b"])
        assertEquals("xxx", instance1["c"])
        assertEquals("yyy", instance2["c"])
    }
}
