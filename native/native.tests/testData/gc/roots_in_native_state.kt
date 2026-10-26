// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi,kotlin.native.concurrent.ObsoleteWorkersApi

// A thread that is in the Native state (blocked, or inside a runtime function that writes to the
// console) is not stopped by the GC: the collector scans its stack while the thread is away. The
// roots of the Kotlin frames of that thread must be known for the whole time of the Native window.
//
// A second thread collects continuously while this one is inside the window.

import kotlin.concurrent.AtomicInt
import kotlin.native.NoInline
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class Node(val data: IntArray) {
    fun sum() = data.sum()
}

@NoInline
private fun newNode(): Node = Node(IntArray(16) { it })

private fun startCollector(stop: AtomicInt, rounds: AtomicInt): Pair<Worker, kotlin.native.concurrent.Future<Unit>> {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { stop to rounds }) { (flag, finishedRounds) ->
        while (flag.value == 0) {
            GC.collect()
            finishedRounds.incrementAndGet()
        }
    }
    return worker to future
}

@Test
fun rootSurvivesConsoleWrites() {
    val stop = AtomicInt(0)
    val rounds = AtomicInt(0)
    val (worker, future) = startCollector(stop, rounds)
    val node = newNode()
    val weak = WeakReference(node)

    // The collections must overlap with the writes, so the loop goes on until the collector has finished
    // a few rounds after the loop started. How soon the collector thread starts depends on the machine load,
    // which is why the number of writes is not fixed. The deadline keeps the test finite if the collector is stuck.
    val roundsBefore = rounds.value
    val deadline = TimeSource.Monotonic.markNow() + 60.seconds
    var writes = 0
    while (writes < 500 || (rounds.value < roundsBefore + 3 && deadline.hasNotPassedNow())) {
        // The runtime switches this thread to the Native state around the write.
        print(writes % 10)
        assertNotNull(weak.value)
        writes++
    }
    println()

    stop.value = 1
    future.result
    assertTrue(rounds.value >= roundsBefore + 3, "the collector finished only ${rounds.value - roundsBefore} rounds during $writes writes")
    worker.requestTermination().result
    assertEquals(120, node.sum())
}

@Test
fun rootSurvivesBlockingOnFuture() {
    val worker = Worker.start()
    val node = newNode()
    val weak = WeakReference(node)

    // The job collects many times, this thread waits for it in the Native state.
    val result = worker.execute(TransferMode.SAFE, { 50 }) { rounds ->
        repeat(rounds) { GC.collect() }
        rounds
    }.result

    assertEquals(50, result)
    assertNotNull(weak.value)
    assertEquals(120, node.sum())
    worker.requestTermination().result
}
