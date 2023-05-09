@file:OptIn(kotlin.ExperimentalStdlibApi::class, ObsoleteWorkersApi::class)

package runtime.workers.worker_list_workers

import kotlin.native.concurrent.*
import kotlin.test.*

const val WORKER_COUNT = 10

@Test
fun getAllWorkers() {
    val workers = Array(WORKER_COUNT) { Worker.start() }

    val expectedWorkers = listOf(Worker.current) + workers

    assertEquals(expectedWorkers.toSet(), Worker.activeWorkers.toSet())

    workers.forEach {
        it.requestTermination().result
    }
}

@Test
fun getActiveWorkers() {
    val workers = Array(WORKER_COUNT) { Worker.start() }

    val expectedWorkers = mutableListOf(Worker.current)
    (0 until WORKER_COUNT step 2).forEach { i ->
        workers[i].requestTermination().result
        expectedWorkers.add(workers[i + 1])
    }

    assertEquals(expectedWorkers.toSet(), Worker.activeWorkers.toSet())

    (0 until WORKER_COUNT step 2).forEach { i ->
        workers[i + 1].requestTermination().result
    }
}
