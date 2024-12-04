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
    fun `test - adding already existing elements`() {
        val set = MutableObservableSetImpl(1, 2, 3)
        val forAllListener = TestListener()
        val whenObjectAddedListener = TestListener()
        set.forAll(forAllListener)
        set.whenObjectAdded(whenObjectAddedListener)

        assertEquals(listOf(1, 2, 3), forAllListener.invocations)
        assertEquals(listOf(), whenObjectAddedListener.invocations)

        /* Adding already existing elements */
        set.add(1)
        set.addAll(listOf(1, 2))

        /* No further invocations */
        assertEquals(listOf(1, 2, 3), forAllListener.invocations)
        assertEquals(listOf(), whenObjectAddedListener.invocations)
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