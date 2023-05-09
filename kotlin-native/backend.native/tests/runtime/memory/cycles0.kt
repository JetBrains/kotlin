/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.cycles0

import kotlin.test.*

data class Node(val data: Int, var next: Node?, var prev: Node?, val outer: Node?)

fun makeCycle(len: Int, outer: Node?): Node {
    val start = Node(0, null, null, outer)
    var prev = start
    for (i in 1 .. len - 1) {
        prev = Node(i, prev, null, outer)
    }
    start.next = prev
    return start
}

fun makeDoubleCycle(len: Int): Node {
    val start = makeCycle(len, null)
    var prev = start
    var cur = prev.next
    while (cur != start) {
        cur!!.prev = prev
        prev = cur
        cur = cur.next
    }
    start.prev = prev
    return start
}

fun createCycles(junk: Node) {
    val cycle1 = makeCycle(1, junk)
    val cycle2 = makeCycle(2, junk)
    val cycle10 = makeCycle(10, junk)
    val cycle100 = makeCycle(100, junk)
    val dcycle1 = makeDoubleCycle(1)
    val dcycle2 = makeDoubleCycle(2)
    val dcycle10 = makeDoubleCycle(10)
    val dcycle100 = makeDoubleCycle(100)

}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Test fun runTest() {
    // Create outer link from cyclic garbage.
    val outer = Node(42, null, null, null)
    createCycles(outer)
    kotlin.native.runtime.GC.collect()
    // Ensure outer is not collected.
    println(outer.data)
}
