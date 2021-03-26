/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.test.Test
import kotlin.test.assertEquals

class TransitiveClosureTest {
    private class Node(val value: String, val children: MutableList<Node> = mutableListOf()) {

        override fun toString(): String {
            return value
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Node) return false
            return other.value == value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }

    private fun Node.transitiveClosure() = org.jetbrains.kotlin.commonizer.util.transitiveClosure(this) { children }

    @Test
    fun `transitiveClosure does not include root node`() {
        val closure = Node("a", mutableListOf(Node("b"), Node("c"))).transitiveClosure()
        assertEquals(setOf(Node("b"), Node("c")), closure, "Expected transitiveClosure to not include root node")
    }

    @Test
    fun `transitiveClosure handles loop and self references`() {
        val nodeA = Node("a")
        val nodeB = Node("b")
        val nodeC = Node("c")
        val nodeD = Node("d")

        // a -> b -> c -> d
        nodeA.children.add(nodeB)
        nodeB.children.add(nodeC)
        nodeC.children.add(nodeD)

        // add self reference to b
        nodeB.children.add(nodeB)

        // add loop from c -> a
        nodeC.children.add(nodeA)

        val closure = nodeA.transitiveClosure()
        assertEquals(
            setOf(nodeB, nodeC, nodeD), closure,
            "Expected transitiveClosure to be robust against loops and self references"
        )
    }
}
