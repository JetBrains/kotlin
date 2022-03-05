/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import org.junit.Test
import kotlin.test.assertEquals

class ClosureTest {

    private class Node(val value: String, var parent: Node? = null, val children: MutableList<Node> = mutableListOf()) {
        override fun toString(): String = value
    }

    @Test
    fun `closure does not include root node`() {
        val closure = Node("a", children = mutableListOf(Node("b"), Node("c"))).closure { it.children }
        assertEquals(
            listOf("b", "c"), closure.map { it.value },
            "Expected closure to not include root node"
        )
    }

    @Test
    fun `withClosure does include root node`() {
        val closure = Node("a", children = mutableListOf(Node("b"), Node("c"))).withClosure { it.children }
        assertEquals(
            listOf("a", "b", "c"), closure.map { it.value },
            "Expected 'withClosure' to include root node"
        )
    }

    @Test
    fun `closure handles loop and self references`() {
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

        val closure = nodeA.closure { it.children }
        assertEquals(
            setOf(nodeB, nodeC, nodeD), closure,
            "Expected transitiveClosure to be robust against loops and self references"
        )
    }

    @Test
    fun `withClosure handles loop and self references`() {
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

        val closure = nodeA.withClosure { it.children }
        assertEquals(
            setOf(nodeA, nodeB, nodeC, nodeD), closure,
            "Expected transitiveClosure to be robust against loops and self references"
        )
    }

    @Test
    fun `closure on List`() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")
        val d = Node("d")
        val e = Node("e")
        val f = Node("f")
        val g = Node("g")

        // a -> b, c -> d
        a.children.add(b)
        a.children.add(c)
        c.children.add(d)

        // e -> f, g, a
        e.children.add(f)
        e.children.add(g)
        e.children.add(a) // <- cycle back to a!

        assertEquals(
            listOf("b", "c", "d", "f", "g"), // <- a *is not* listed in closure!!!
            listOf(a, e).closure<Node> { it.children }.map { it.value }
        )
    }

    @Test
    fun `withClosure on List`() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")
        val d = Node("d")
        val e = Node("e")
        val f = Node("f")
        val g = Node("g")

        // a -> b, c -> d
        a.children.add(b)
        a.children.add(c)
        c.children.add(d)

        // e -> f, g, a
        e.children.add(f)
        e.children.add(g)
        e.children.add(a) // <- cycle back to a!

        assertEquals(
            listOf("a", "e", "b", "c", "d", "f", "g"),
            listOf(a, e).withClosure<Node> { it.children }.map { it.value }
        )
    }

    @Test
    fun singleClosure() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")

        c.parent = b
        b.parent = a

        assertEquals(
            listOf("b", "a"), c.singleClosure { it.parent }.map { it.value },
        )
    }

    @Test
    fun withSingleClosure() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")

        c.parent = b
        b.parent = a

        assertEquals(
            listOf("c", "b", "a"), c.withSingleClosure { it.parent }.map { it.value },
        )
    }
}
