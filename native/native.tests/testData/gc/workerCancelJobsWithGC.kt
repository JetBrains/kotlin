// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi,kotlin.native.runtime.NativeRuntimeApi,kotlin.native.internal.InternalForKotlinNative

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.runtime.GC
import kotlin.test.*

const val WORKER_COUNT = 100

@Test
fun runTest() {
    val canUnblockQueue = AtomicInt(0)

    val workers = Array(WORKER_COUNT) { Worker.start(name = "Worker #$it") }

    val terminationFutures = workers.map {
        it.execute(TransferMode.SAFE, { canUnblockQueue }) { canUnblockQueue ->
            while (canUnblockQueue.value == 0) {}
        }
        val terminateRequestFuture = it.requestTermination()
        it.execute(TransferMode.SAFE, {}) { error("Executed job after termination") }
        terminateRequestFuture
    }

    // All queues are ready. Unblock the first task, schedule GC, and wait for all termination requests.
    GC.schedule()
    canUnblockQueue.value = 1
    terminationFutures.forEach { it.result }

    // And now wait for all the workers to complete termination.
    workers.forEach { waitWorkerTermination(it) }
}