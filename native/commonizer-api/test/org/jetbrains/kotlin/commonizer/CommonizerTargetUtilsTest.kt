/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonizerTargetUtilsTest {

    @Test
    fun `isAncestorOf isDescendentOf`() {
        val child = LeafCommonizerTarget("a")
        val parent = SharedCommonizerTarget(LeafCommonizerTarget("a"))

        assertTrue(child isDescendentOf parent, "Expected child isDescendent of parent")
        assertTrue(parent isAncestorOf child, "Expected parent isAncestor of child")
        assertFalse(child isDescendentOf child, "Expected same target not being descendent of itself")
        assertFalse(parent isDescendentOf parent, "Expected same target not being descendent of itself")
        assertFalse(LeafCommonizerTarget("b") isDescendentOf parent, "Expected orphan target not being descendent of parent")

        val hierarchicalParent = SharedCommonizerTarget(parent, parseCommonizerTarget("((c, d), e)"))
        assertTrue(child isDescendentOf hierarchicalParent, "Expected child being descendent of hierarchical parent")
        assertTrue(hierarchicalParent isAncestorOf child, "Expected hierarchicalParent being ancestor of child")
        assertTrue(parseCommonizerTarget("(c, d)") isDescendentOf hierarchicalParent)
        assertTrue(LeafCommonizerTarget("e") isDescendentOf hierarchicalParent)
    }

    @Test
    fun withAllAncestors() {
        val target = parseCommonizerTarget("((a, b), (c, d), (e, (f, g)))")

        assertEquals(
            setOf(
                target,
                parseCommonizerTarget("(a, b)"),
                parseCommonizerTarget("(c, d)"),
                parseCommonizerTarget("(e, (f, g))"),
                parseCommonizerTarget("(f, g)"),
                LeafCommonizerTarget("a"),
                LeafCommonizerTarget("b"),
                LeafCommonizerTarget("c"),
                LeafCommonizerTarget("d"),
                LeafCommonizerTarget("e"),
                LeafCommonizerTarget("f"),
                LeafCommonizerTarget("g")
            ),
            target.withAllAncestors(),
            "Expected all targets present"
        )
    }

    @Test
    fun allLeaves() {
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
}

