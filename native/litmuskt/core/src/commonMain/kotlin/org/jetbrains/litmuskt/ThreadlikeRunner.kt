package org.jetbrains.litmuskt

abstract class ThreadlikeRunner : LitmusRunner() {

    protected abstract fun threadlikeProducer(): Threadlike

    private fun <S : Any> threadFunction(threadContext: ThreadContext<S>) = with(threadContext) {
        for (i in states.indices) {
            if (i % syncPeriod == 0) barrier.await()
            states[i].testThreadFunction()
        }
    }

    private data class ThreadContext<S : Any>(
        val states: Array<S>,
        val testThreadFunction: S.() -> Unit,
        val syncPeriod: Int,
        val barrier: Barrier,
    )

    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: Array<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): () -> LitmusResult {

        val threads = List(test.threadCount) { threadlikeProducer() }
        affinityMap?.let { map ->
            affinityManager?.apply {
                for ((i, t) in threads.withIndex()) {
                    setAffinityAndCheck(t, map.allowedCores(i))
                }
            }
        }

        val barrier = barrierProducer(test.threadCount)
        val contexts = List(threads.size) { i ->
            ThreadContext(states, test.threadFunctions[i], syncPeriod, barrier)
        }

        val futures = (threads zip contexts).map { (thread, context) ->
            thread.start(context, ::threadFunction)
        }

        return {
            futures.forEach { it.await() } // await all results
            threads.forEach { it.dispose() } // stop all "threads"
            calcStats(states, test.outcomeSpec, test.outcomeFinalizer)
        }
    }
}
