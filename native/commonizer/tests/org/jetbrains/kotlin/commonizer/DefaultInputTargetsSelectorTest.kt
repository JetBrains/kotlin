/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultInputTargetsSelectorTest {

    @Test
    fun `missing leaf targets`() {
        val inputTargets = setOf(LeafCommonizerTarget("a"), LeafCommonizerTarget("b"))

        val exception = assertFailsWith<IllegalArgumentException> {
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b, c)") as SharedCommonizerTarget)
        }

        assertTrue(
            exception.message.orEmpty().contains(inputTargets.toString()),
            "Expected error message to contain all input targets. Found ${exception.message}"
        )

        assertTrue(
            exception.message.orEmpty().contains(parseCommonizerTarget("(a, b, c)").toString()),
            "Expected error message to contain output target. Found ${exception.message}"
        )
    }

    @Test
    fun `sample 0`() {
        val inputTargets = setOf(
            LeafCommonizerTarget("a"),
            LeafCommonizerTarget("b"),
            LeafCommonizerTarget("c"),
            LeafCommonizerTarget("d"),
            SharedCommonizerTarget(LeafCommonizerTarget("c"), LeafCommonizerTarget("d"))
        )

        assertEquals(
            setOf(LeafCommonizerTarget("a"), LeafCommonizerTarget("b")),
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b)") as SharedCommonizerTarget)
        )

        assertEquals(
            setOf(LeafCommonizerTarget("a"), LeafCommonizerTarget("b"), LeafCommonizerTarget("c")),
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b, c)") as SharedCommonizerTarget)
        )

        assertEquals(
            setOf(LeafCommonizerTarget("a"), LeafCommonizerTarget("b"), parseCommonizerTarget("(c, d)")),
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b, c, d)") as SharedCommonizerTarget)
        )
    }

    @Test
    fun `sample 1`() {
        val inputTargets = setOf(
            parseCommonizerTarget("(a, b)"),
            parseCommonizerTarget("(a, b, c)"),
            parseCommonizerTarget("(a, b, c, d)"),
            parseCommonizerTarget("(c, d)"),
            parseCommonizerTarget("(c, d, e)"),
            parseCommonizerTarget("f")
        )

        assertEquals(
            setOf("(a, b, c, d)", "f").map(::parseCommonizerTarget).toSet(),
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b, c, d, f)") as SharedCommonizerTarget)
        )

        assertEquals(
            setOf("(a, b, c, d)", "(c, d, e)").map(::parseCommonizerTarget).toSet(),
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b, c, d, e)") as SharedCommonizerTarget)
        )

        assertEquals(
            setOf("(a, b, c, d)", "(c, d, e)", "f").map(::parseCommonizerTarget).toSet(),
            DefaultInputTargetsSelector(inputTargets, parseCommonizerTarget("(a, b, c, d, e, f)") as SharedCommonizerTarget)
        )
    }

    @Test
    fun `empty outputTarget`() {
        assertEquals(
            emptySet(),
            DefaultInputTargetsSelector(emptySet(), parseCommonizerTarget("()") as SharedCommonizerTarget)
        )
    }

    @Test
    fun `single leaf outputTarget`() {
        assertEquals(
            setOf(LeafCommonizerTarget("a")),
            DefaultInputTargetsSelector(setOf(LeafCommonizerTarget("a")), parseCommonizerTarget("(a)") as SharedCommonizerTarget)
        )
    }

    @Test
    fun `exact output available`() {
        assertEquals(
            setOf(parseCommonizerTarget("(a, b)"), parseCommonizerTarget("(c, d)")),
            DefaultInputTargetsSelector(
                setOf("a", "b", "c", "d", "(a, b)", "(c, d)", "(a, b, c, d)").map(::parseCommonizerTarget).toSet(),
                parseCommonizerTarget("(a, b, c, d)") as SharedCommonizerTarget
            )
        )
    }
}