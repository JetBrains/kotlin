/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class MergeWithTest {
    private fun Map<String, Set<Int>>.prettyStringForDiff() = entries
        .sortedBy { it.key }
        .joinToString("\n") { (key, setOfInts) -> "$key => ${setOfInts.sorted()}" }

    private fun assertEquals(expected: Map<String, Set<Int>>, actual: Map<String, Set<Int>>) {
        assertEquals(expected.prettyStringForDiff(), actual.prettyStringForDiff())
    }

    private val sample = mapOf(
        "a" to setOf(1, 2),
        "b" to setOf(3, 4),
        "c" to emptySet(),
    )


    @Test
    fun basicTest() {
        val a = mapOf(
            "a" to setOf(1, 2),
            "b" to setOf(3, 4)
        )

        val b = mapOf(
            "b" to setOf(3, 5),
            "c" to setOf(6),
            "d" to emptySet(),
        )

        val actual = a mergeWith b
        val expected = mapOf(
            "a" to setOf(1, 2),
            "b" to setOf(3, 4, 5),
            "c" to setOf(6),
            "d" to emptySet()
        )

        assertEquals(expected, actual)
    }

    @Test
    fun mergeWithSelf() = assertEquals(sample, sample mergeWith sample)

    @Test
    fun mergeWithEmpty() {
        assertEquals(sample, emptyMap<String, Set<Int>>() mergeWith sample)
        assertEquals(sample, sample mergeWith emptyMap())
        assertEquals(emptyMap(), emptyMap<String, Set<Int>>() mergeWith emptyMap())
    }
}