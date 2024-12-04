/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.test.Test
import kotlin.test.assertEquals

public class CommonizerTargetUtilsTest {

    @Test
    public fun allLeaves() {
        val target = parseCommonizerTarget("((a, b), (c, d), (e, (f, g)))")
        assertEquals(
            setOf(
                LeafCommonizerTarget("a"),
                LeafCommonizerTarget("b"),
                LeafCommonizerTarget("c"),
                LeafCommonizerTarget("d"),
                LeafCommonizerTarget("e"),
                LeafCommonizerTarget("f"),
                LeafCommonizerTarget("g")
            ),
            target.allLeaves(),
            "Expected leaf targets present"
        )

        assertEquals(
            setOf(LeafCommonizerTarget("a")), LeafCommonizerTarget("a").allLeaves(),
            "Expected LeafCommonizerTarget returns itself in 'allLeaves'"
        )
    }

    @Test
    public fun `withAllLeaves LeafCommonizerTarget`() {
        assertEquals(setOf(LeafCommonizerTarget("a")), LeafCommonizerTarget("a").withAllLeaves())
    }

    @Test
    public fun `withAllLeaves SharedCommonizerTarget`() {
        assertEquals(
            setOf(parseCommonizerTarget("(a, b)"), LeafCommonizerTarget("a"), LeafCommonizerTarget("b")),
            parseCommonizerTarget("(a, b)").withAllLeaves()
        )
    }
}

