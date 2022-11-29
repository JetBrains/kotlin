/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import org.jetbrains.kotlin.tooling.core.HasExtrasTest.Subject.Companion.value1
import org.jetbrains.kotlin.tooling.core.HasExtrasTest.Subject.Companion.value2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HasExtrasTest {

    class Subject : HasMutableExtras {
        override val extras: MutableExtras = mutableExtrasOf()

        companion object {
            var Subject.value1: Int? by extrasKeyOf()
            var Subject.value2: Int? by extrasKeyOf("2")
        }
    }

    @Test
    fun `test - non-null value`() {
        val subject = Subject()
        assertNull(subject.value1)
        assertNull(subject.value2)

        subject.value1 = 1
        assertEquals(1, subject.value1)
        assertNull(subject.value2)

        subject.value2 = 2
        assertEquals(1, subject.value1)
        assertEquals(2, subject.value2)
    }

    @Test
    fun `test - set null`() {
        val subject = Subject()
        subject.value1 = 1
        assertEquals(1, subject.value1)

        subject.value1 = null
        assertNull(subject.value1)
    }
}
