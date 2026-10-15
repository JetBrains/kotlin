// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -Xbinary=gcSchedulerType=aggressive -opt-in=kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi,kotlin.native.concurrent.ObsoleteWorkersApi

// The aggressive scheduler collects at almost every safe point, so the collection stops the thread
// at an arbitrary place of Kotlin code: a function entry, a loop back edge, a call. A program that
// builds and checks data structures must give correct results: a root that the collector misses
// shows as a corrupted or collected structure.

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.*

class Tree(val left: Tree?, val right: Tree?)

private fun build(depth: Int): Tree =
        if (depth == 0) Tree(null, null) else Tree(build(depth - 1), build(depth - 1))

private fun count(tree: Tree): Int = 1 + (tree.left?.let { count(it) } ?: 0) + (tree.right?.let { count(it) } ?: 0)

// 2^(depth + 1) - 1
private fun nodes(depth: Int) = (1 shl (depth + 1)) - 1

@Test
fun treesBuiltUnderAggressiveCollection() {
    val longLived = build(12)

    repeat(40) {
        val tree = build(8)
        assertEquals(nodes(8), count(tree))
    }

    assertEquals(nodes(12), count(longLived))
}

@Test
fun stringsAndListsBuiltUnderAggressiveCollection() {
    val lists = ArrayList<List<String>>()

    repeat(200) { i ->
        val list = ArrayList<String>()
        repeat(20) { j -> list.add("item-" + i.toString() + "-" + j.toString()) }
        lists.add(list)
    }

    for (i in 0 until 200) {
        for (j in 0 until 20) {
            assertEquals("item-" + i.toString() + "-" + j.toString(), lists[i][j])
        }
    }
}

@Test
fun workersBuildTreesUnderAggressiveCollection() {
    val workers = List(3) { Worker.start() }
    val futures = workers.map { worker ->
        worker.execute(TransferMode.SAFE, { 10 }) { depth ->
            var total = 0
            repeat(10) { total += count(build(depth)) }
            total
        }
    }

    futures.forEach { assertEquals(10 * nodes(10), it.result) }
    workers.forEach { it.requestTermination().result }
}
