/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze_stress

import kotlin.test.*

import kotlin.native.concurrent.*

class Random(private var seed: Int) {
    fun next(): Int {
        seed = (1103515245 * seed + 12345) and 0x7fffffff
        return seed
    }

    fun next(maxExclusiveValue: Int) = if (maxExclusiveValue == 0) 0 else next() % maxExclusiveValue

    fun next(minInclusiveValue: Int, maxInclusiveValue: Int) =
            minInclusiveValue + next(maxInclusiveValue - minInclusiveValue + 1)
}

class Node(val id: Int) {
    var numberOfEdges = 0

    lateinit var edge0: Node
    lateinit var edge1: Node
    lateinit var edge2: Node
    lateinit var edge3: Node
    lateinit var edge4: Node
    lateinit var edge5: Node
    lateinit var edge6: Node
    lateinit var edge7: Node
    lateinit var edge8: Node
    lateinit var edge9: Node

    fun addEdge(child: Node) {
        when (numberOfEdges) {
            0 -> edge0 = child
            1 -> edge1 = child
            2 -> edge2 = child
            3 -> edge3 = child
            4 -> edge4 = child
            5 -> edge5 = child
            6 -> edge6 = child
            7 -> edge7 = child
            8 -> edge8 = child
            9 -> edge9 = child
            else -> error("Too many edges")
        }
        ++numberOfEdges
    }

    fun getEdges(): List<Node> {
        val result = mutableListOf<Node>()
        if (numberOfEdges > 0) result += edge0
        if (numberOfEdges > 1) result += edge1
        if (numberOfEdges > 2) result += edge2
        if (numberOfEdges > 3) result += edge3
        if (numberOfEdges > 4) result += edge4
        if (numberOfEdges > 5) result += edge5
        if (numberOfEdges > 6) result += edge6
        if (numberOfEdges > 7) result += edge7
        if (numberOfEdges > 8) result += edge8
        if (numberOfEdges > 9) result += edge9
        return result
    }
}

class Graph(val nodes: List<Node>, val roots: List<Node>)

fun min(x: Int, y: Int) = if (x < y) x else y
fun max(x: Int, y: Int) = if (x > y) x else y

@ThreadLocal
val random = Random(42)

fun generate(condensationSize: Int, branchingFactor: Int, swellingFactor: Int): Graph {
    var id = 0
    val nodes = mutableListOf<Node>()

    fun genDAG(n: Int): Node {
        val node = Node(id++)
        nodes += node
        if (n == 1) return node
        val numberOfChildren = random.next(1, min(n - 1, branchingFactor))
        val used = BooleanArray(n)
        val points = IntArray(numberOfChildren + 1)
        points[0] = 0
        points[numberOfChildren] = n - 1
        used[0] = true
        used[n - 1] = true
        for (i in 1 until numberOfChildren) {
            var p: Int
            do {
                p = random.next(1, n - 1)
            } while (used[p])
            used[p] = true
            points[i] = p
        }
        points.sort()
        for (i in 1..numberOfChildren) {
            val childSize = points[i] - points[i - 1]
            val child = genDAG(childSize)
            if (random.next(2) == 0)
                node.addEdge(child)
            else
                child.addEdge(node)
        }
        return node
    }

    genDAG(condensationSize)

    val numberOfEnters = IntArray(condensationSize)
    for (node in nodes)
        for (edge in node.getEdges())
            ++numberOfEnters[edge.id]
    val roots = nodes.filter { numberOfEnters[it.id] == 0 }
    for (i in 0 until condensationSize) {
        val node = nodes[i]
        val componentSize = random.next(1, swellingFactor)
        if (componentSize == 1 && random.next(2) == 0)
            continue
        val component = Array(componentSize) {
            if (it == 0) node else Node(id++).also { nodes += it }
        }
        for (j in 0 until componentSize)
            component[j].addEdge(component[(j - 1 + componentSize) % componentSize])
        val numberOfAdditionalEdges = random.next((componentSize + 1) / 2)
        for (j in 0 until numberOfAdditionalEdges)
            component[random.next(componentSize)].addEdge(component[random.next(componentSize)])
    }

    return Graph(nodes, roots)
}

fun freezeOneGraph() {
    val graph = generate(100, 5, 20)
    graph.roots.forEach { it.freeze() }
    for (node in graph.nodes)
        assert (node.isFrozen, { "All nodes should be frozen" })
}

@Test fun runTest() {
    for (i in 0..1000) {
        freezeOneGraph()
    }
    println("OK")
}