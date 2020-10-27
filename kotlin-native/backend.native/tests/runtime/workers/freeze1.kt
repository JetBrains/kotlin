/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze1

import kotlin.test.*

import kotlin.native.concurrent.*

data class Node(var previous: Node?, var data: Int)

fun makeCycle(count: Int): Node {
    val first = Node(null, 0)
    var current = first
    for (index in 1 .. count - 1) {
        current = Node(current, index)
    }
    first.previous = current
    return first
}

data class Node2(var leaf1: Node2?, var leaf2: Node2?)

fun makeDiamond(): Node2 {
    val bottom = Node2(null, null)
    val mid1prime = Node2(bottom, null)
    val mid1 = Node2(mid1prime, null)
    val mid2 = Node2(bottom, null)
    return Node2(mid1, mid2)
}

@Test fun runTest() {
    makeCycle(10).freeze()

    // Must be able to freeze diamond shaped graph.
    val diamond = makeDiamond().freeze()

    val immutable = Node(null, 4).freeze()
    try {
        immutable.data = 42
    } catch (e: InvalidMutabilityException) {
        println("OK, cannot mutate frozen")
    }
}