package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.*
import org.jetbrains.litmuskt.*
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

/**
 * A runner based on Kotlin/Native Workers. They are declared obsolete,
 * but an alternative is yet to exist.
 */
class WorkerRunner : LitmusRunner() {

    @OptIn(ObsoleteWorkersApi::class)
    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: Array<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): () -> LitmusResult {

        data class WorkerContext(
            val states: Array<S>,
            val threadFunction: S.() -> Any?,
            val syncPeriod: Int,
            val barrier: Barrier,
        )

        val barrier = barrierProducer(test.threadCount)
        val workers = List(test.threadCount) { Worker.start() }

        val futures = workers.mapIndexed { threadIndex, worker ->
            affinityMap?.let { affinityMap ->
                org.jetbrains.litmuskt.getAffinityManager()?.run {
                    val cpuSet = affinityMap.allowedCores(threadIndex)
                    setAffinity(worker, cpuSet)
                    require(getAffinity(worker) == cpuSet) { "affinity setting failed" }
                }
            }
            val workerContext = WorkerContext(
                states,
                test.threadFunctions[threadIndex],
                syncPeriod,
                barrier,
            )
            worker.execute(
                TransferMode.SAFE /* ignored */,
                { workerContext }
            ) { (states, threadFunction, syncPeriod, barrier) ->
                for (i in states.indices) {
                    if (i % syncPeriod == 0) barrier.await()
                    states[i].threadFunction()
                }
            }
        }

        return {
            futures.forEach { it.result } // await all results
            workers.forEach { it.requestTermination().result } // waits for all workers to stop
            calcStats(states, test.outcomeSpec, test.outcomeFinalizer)
        }
    }
}
