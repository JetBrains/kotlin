/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.utils.MutableObservableSetImpl
import org.junit.Test
import kotlin.test.assertEquals

class ObservableSetTest {

    class TestListener : (Int) -> Unit {
        val invocations = mutableListOf<Int>()
        override fun invoke(p1: Int) {
            invocations.add(p1)
        }
    }

    @Test
    fun `test - forAll`() {
        val set = MutableObservableSetImpl(1)
        val testListener1 = TestListener()
        val testListener2 = TestListener()

        set.forAll(testListener1)
        assertEquals(listOf(1), testListener1.invocations)

        set.add(2)
        assertEquals(listOf(1, 2), testListener1.invocations)

        set.addAll(listOf(3, 4))
        assertEquals(listOf(1, 2, 3, 4), testListener1.invocations)

        set.forAll(testListener2)
        assertEquals(listOf(1, 2, 3, 4), testListener2.invocations)
    }

    @Test
    fun `test - whenObjectAdded`() {
        val set = MutableObservableSetImpl(1)
        val testListener1 = TestListener()
        val testListener2 = TestListener()

        set.whenObjectAdded(testListener1)
        assertEquals(emptyList(), testListener1.invocations)

        set.add(2)
        assertEquals(listOf(2), testListener1.invocations)

        set.addAll(listOf(3, 4))
        assertEquals(listOf(2, 3, 4), testListener1.invocations)

        set.whenObjectAdded(testListener2)
        assertEquals(emptyList(), testListener2.invocations)
    }
}