package org.jetbrains.litmuskt

abstract class ThreadlikeRunner : LitmusRunner() {

    protected abstract fun threadlikeProducer(): Threadlike

    private fun <S : Any> threadFunction(threadContext: ThreadContext<S>) = with(threadContext) {
        val testFunction = test.threadFunctions[threadIndex]
        for (i in states.indices) {
            if (i % syncPeriod == 0) barrier.await()
            states[i].testFunction()
        }
        // performance optimization: each thread takes a portion of states and calculates stats for it
        rangeResult = calcStats(states.view(resultCalcRange), test.outcomeSpec, test.outcomeFinalizer)
    }

    private class ThreadContext<S : Any>(
        val states: Array<S>,
        val test: LitmusTest<S>,
        val threadIndex: Int,
        val syncPeriod: Int,
        val barrier: Barrier,
        val resultCalcRange: IntRange,
        var rangeResult: LitmusResult? = null
    )

    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: Array<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): BlockingFuture<LitmusResult> {

        val threads = List(test.threadCount) { threadlikeProducer() }

        val barrier = barrierProducer(test.threadCount)
        val resultCalcRanges = states.indices.splitEqual(threads.size)
        val contexts = List(threads.size) { i ->
            val range = resultCalcRanges[i]
            ThreadContext(states, test, i, syncPeriod, barrier, range)
        }

        val futures = (threads zip contexts).map { (thread, context) ->
            thread.start(context, ::threadFunction)
        }

        // cannot set affinity before thread is started (because pthread_create has not been called yet)
        affinityMap?.let { map ->
            affinityManager?.apply {
                for ((i, t) in threads.withIndex()) {
                    setAffinityAndCheck(t, map.allowedCores(i))
                }
            }
        }

        return BlockingFuture {
            futures.forEach { it.await() } // await all results
            threads.forEach { it.dispose() } // stop all "threads"
            contexts.map { it.rangeResult!! }.mergeResults()
        }
    }
}
