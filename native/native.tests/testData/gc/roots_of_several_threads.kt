// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi,kotlin.native.concurrent.ObsoleteWorkersApi

// Several threads keep objects in their frames and ask for collections. A collection that is
// started by one thread must find the roots of all the other threads: of those that run Kotlin
// code, and of those that wait in the Native state.

import kotlin.native.NoInline
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.test.*

class Node(val data: IntArray) {
    fun sum() = data.sum()
}

@NoInline
private fun newNode(): Node = Node(IntArray(16) { it })

// Runs on a worker: holds nodes only in its frame, collects, and counts the rounds in which all
// of them are still alive.
@NoInline
private fun holdAndCollect(rounds: Int): Int {
    val nodes = Array<Node?>(50) { newNode() }
    val weaks = nodes.map { WeakReference(it!!) }
    var aliveRounds = 0
    repeat(rounds) {
        GC.collect()
        if (weaks.all { it.value != null }) aliveRounds++
    }
    check(nodes.sumOf { it!!.sum() } == 50 * 120)
    return aliveRounds
}

@Test
fun rootsOfAllThreadsSurviveCollectionsOfEveryThread() {
    val workers = List(4) { Worker.start() }
    val futures = workers.map { worker ->
        worker.execute(TransferMode.SAFE, { 25 }) { rounds -> holdAndCollect(rounds) }
    }

    // This thread collects and holds roots as well.
    val mine = holdAndCollect(25)

    assertEquals(25, mine)
    futures.forEach { assertEquals(25, it.result) }
    workers.forEach { it.requestTermination().result }
}

@Test
fun threadsStartAndFinishWhileOthersCollect() {
    val node = newNode()
    val weak = WeakReference(node)

    repeat(10) {
        val worker = Worker.start()
        val result = worker.execute(TransferMode.SAFE, { it }) { value ->
            GC.collect()
            value + 1
        }.result
        assertEquals(it + 1, result)
        worker.requestTermination().result
        GC.collect()
        assertNotNull(weak.value)
    }

    assertEquals(120, node.sum())
}
