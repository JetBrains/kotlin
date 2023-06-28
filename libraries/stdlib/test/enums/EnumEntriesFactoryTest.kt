/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package test.enums

import kotlin.enums.enumEntries
import kotlin.test.*
import test.collections.behaviors.listBehavior
import test.collections.compare
import kotlin.enums.EnumEntries

class EnumEntriesFactoryTest {

    enum class EmptyEnum

    enum class NonEmptyEnum {
        A, B, C
    }

    @Test
    fun testEquality() {
        assertEquals(EmptyEnum.entries, enumEntries())
        assertEquals(NonEmptyEnum.entries, enumEntries())
    }

    @Test
    fun testByCallableReference() {
        val empty: () -> EnumEntries<EmptyEnum> = ::enumEntries
        assertEquals(EmptyEnum.entries, empty())
        val nonEmpty: () -> EnumEntries<NonEmptyEnum> = ::enumEntries
        assertEquals(NonEmptyEnum.entries, nonEmpty())
    }

    @Test
    @Suppress("EnumValuesSoftDeprecate") // For test to avoid comparing entries with entries
    fun testSanity() {
        compare(EnumEntriesListTest.EmptyEnum.values().toList(), enumEntries()) { listBehavior() }
        compare(EnumEntriesListTest.NonEmptyEnum.values().toList(), enumEntries()) { listBehavior() }
    }
}
