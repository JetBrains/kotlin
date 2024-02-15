/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ClosureTest {

    private class Node(val value: String, var parent: Node? = null, val children: MutableList<Node> = mutableListOf()) {
        override fun toString(): String = value
    }

    /* 'Children' is explicitly not implementing Collection */
    private class IterableNode(val value: String, val children: Children = Children(mutableListOf())) {

        /* Does not implement Collection */
        class Children(private val list: MutableList<IterableNode>) : Iterable<IterableNode> {
            override fun iterator(): Iterator<IterableNode> = list.iterator()
            fun add(node: IterableNode) = list.add(node)
        }

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
    fun `closure handles loop and self references - iterable node`() {
        val nodeA = IterableNode("a")
        val nodeB = IterableNode("b")
        val nodeC = IterableNode("c")
        val nodeD = IterableNode("d")

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
    fun `withClosure handles loop and self references - iterable node`() {
        val nodeA = IterableNode("a")
        val nodeB = IterableNode("b")
        val nodeC = IterableNode("c")
        val nodeD = IterableNode("d")

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
    fun `closure with empty nodes`() {
        assertSame(
            emptySet(), Node("").closure { it.children },
            "Expected no Set being allocated on empty closure"
        )
    }

    @Test
    fun `closure with only self reference`() {
        val node = Node("a")
        node.children.add(node)
        assertEquals(emptySet(), node.closure { it.children })
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

        // a -> (b, c)
        // c -> (d)
        a.children.add(b)
        a.children.add(c)
        c.children.add(d)

        // e -> (f, g, a)
        e.children.add(f)
        e.children.add(g)
        e.children.add(a) // <- cycle back to a!

        assertEquals(
            listOf("b", "c", "f", "g", "d"), // <- a *is not* listed in closure!!!
            listOf(a, e).closure<Node> { it.children }.map { it.value }
        )
    }

    @Test
    fun `closure on List - iterable node`() {
        val a = IterableNode("a")
        val b = IterableNode("b")
        val c = IterableNode("c")
        val d = IterableNode("d")
        val e = IterableNode("e")
        val f = IterableNode("f")
        val g = IterableNode("g")

        // a -> (b, c)
        // c -> (d)
        a.children.add(b)
        a.children.add(c)
        c.children.add(d)

        // e -> (f, g, a)
        e.children.add(f)
        e.children.add(g)
        e.children.add(a) // <- cycle back to a!

        assertEquals(
            listOf("b", "c", "f", "g", "d"), // <- a *is not* listed in closure!!!
            listOf(a, e).closure<IterableNode> { it.children }.map { it.value }
        )
    }

    @Test
    fun `closure on empty list`() {
        assertSame(
            emptySet(), listOf<Node>().closure<Node> { it.children },
            "Expected no Set being allocated on empty closure"
        )
    }

    @Test
    fun `closure - on list - no edges`() {
        assertSame(
            emptySet(), listOf(Node("a"), Node("b")).closure<Node> { it.children },
            "Expected no Set being allocated on empty closure"
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

        // a -> (b, c)
        // c -> (d)
        a.children.add(b)
        a.children.add(c)
        c.children.add(d)

        // e -> (f, g, a)
        e.children.add(f)
        e.children.add(g)
        e.children.add(a) // <- cycle back to a!

        assertEquals(
            listOf("a", "e", "b", "c", "f", "g", "d"),
            listOf(a, e).withClosure<Node> { it.children }.map { it.value }
        )
    }

    @Test
    fun `withClosure on emptyList`() {
        assertSame(
            emptySet(), listOf<Node>().withClosure<Node> { it.children },
            "Expected no Set being allocated on empty closure"
        )
    }

    @Test
    fun `withClosure with no further nodes`() {
        assertEquals(
            listOf("a", "b"), listOf(Node("a"), Node("b")).withClosure<Node> { it.children }.map { it.value }
        )
    }

    @Test
    fun linearClosure() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")

        c.parent = b
        b.parent = a

        assertEquals(
            listOf("b", "a"), c.linearClosure { it.parent }.map { it.value },
        )
    }

    @Test
    fun `linearClosure - loop`() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")

        c.parent = b
        b.parent = a
        a.parent = c

        assertEquals(
            listOf("b", "a"), c.linearClosure { it.parent }.map { it.value },
        )
    }

    @Test
    fun `linearClosure on empty`() {
        assertSame(
            emptySet(), Node("").linearClosure { it.parent },
            "Expected no Set being allocated on empty linearClosure"
        )
    }

    @Test
    fun withLinearClosure() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")

        c.parent = b
        b.parent = a

        assertEquals(
            listOf("c", "b", "a"), c.withLinearClosure { it.parent }.map { it.value },
        )
    }

    @Test
    fun `withLinearClosure - loop`() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")

        c.parent = b
        b.parent = a
        a.parent = c

        assertEquals(
            listOf("c", "b", "a"), c.withLinearClosure { it.parent }.map { it.value },
        )
    }

    @Test
    fun `withLinearClosure on empty`() {
        assertEquals(
            listOf("a"), Node("a").withLinearClosure { it.parent }.map { it.value },
        )
    }

    @Test
    fun `withClosureGroupingByDistance on List`() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")
        val d = Node("d")
        val e = Node("e")
        val f = Node("f")
        val g = Node("g")

        // a -> (b, c)
        // c -> (d)
        a.children.add(b)
        a.children.add(c)
        c.children.add(d)

        // e -> (f, g, a)
        e.children.add(f)
        e.children.add(g)
        e.children.add(a) // <- cycle back to a!

        assertEquals(
            listOf(
                listOf("a", "e"),
                listOf("b", "c", "f", "g"),
                listOf("d"),
            ),
            listOf(a, e).withClosureGroupingByDistance<Node> { it.children }.map { it.map { it.value } }
        )
    }


    @Test
    fun `withClosureGroupingByDistance on emptyList`() {
        assertSame(
            emptyList(), listOf<Node>().withClosureGroupingByDistance<Node> { it.children },
            "Expected no Set being allocated on empty closure"
        )
    }

    @Test
    fun `withClosureGroupingByDistance with no further nodes`() {
        assertEquals(
            listOf(listOf("a", "b")),
            listOf(Node("a"), Node("b")).withClosureGroupingByDistance<Node> { it.children }.map { it.map { it.value } }
        )
    }

    @Test
    fun `withClosureGroupingByDistance handles loop and self references`() {
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

        val closure = listOf(nodeA).withClosureGroupingByDistance { it.children }.map { it.map { it } }
        assertEquals(
            listOf(
                listOf(nodeA),
                listOf(nodeB),
                listOf(nodeC),
                listOf(nodeD)
            ), closure,
            "Expected withClosureGroupingByDistance to be robust against loops and self references"
        )
    }

    @Test
    fun `withClosureGroupingByDistance handles a simple self reference`() {
        val nodeA = Node("a")
        nodeA.children.add(nodeA)

        assertEquals(
            listOf(
                listOf("a")
            ),
            listOf(nodeA).withClosureGroupingByDistance { it.children }.map { it.map { it.value } },
        )
    }

    @Test
    fun `withClosureGroupingByDistance handles a simple cyclic reference`() {
        val nodeA = Node("a")
        val nodeB = Node("b")
        nodeA.children.add(nodeB)
        nodeB.children.add(nodeA)

        assertEquals(
            listOf(
                listOf("a"),
                listOf("b"),
            ),
            listOf(nodeA).withClosureGroupingByDistance { it.children }.map { it.map { it.value } },
        )
        assertEquals(
            listOf(
                listOf("a", "b"),
            ),
            listOf(nodeA, nodeB).withClosureGroupingByDistance { it.children }.map { it.map { it.value } },
        )
    }

    @Test
    fun `withClosureGroupingByDistance handles a loop`() {
        val nodeA = Node("a")
        val nodeB = Node("b")
        val nodeC = Node("c")
        nodeA.children.add(nodeB)
        nodeB.children.add(nodeC)
        nodeC.children.add(nodeA)

        assertEquals(
            listOf(
                listOf("a"),
                listOf("b"),
                listOf("c"),
            ),
            listOf(nodeA).withClosureGroupingByDistance { it.children }.map { it.map { it.value } },
        )
        assertEquals(
            listOf(
                listOf("a", "b"),
                listOf("c"),
            ),
            listOf(nodeA, nodeB).withClosureGroupingByDistance { it.children }.map { it.map { it.value } },
        )
        assertEquals(
            listOf(
                listOf("a", "b", "c"),
            ),
            listOf(nodeA, nodeB, nodeC).withClosureGroupingByDistance { it.children }.map { it.map { it.value } },
        )
    }

}
