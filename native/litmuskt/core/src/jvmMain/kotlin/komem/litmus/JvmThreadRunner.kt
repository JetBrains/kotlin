package komem.litmus

/**
 * A simplistic runner based on JVM threads. Does not support affinity.
 */
class JvmThreadRunner : LitmusRunner() {

    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: List<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): () -> LitmusResult {
        val barrier = barrierProducer(test.threadCount)
        val outcomeFinalizer = test.outcomeFinalizer

        val threads = List(test.threadCount) { threadIndex ->
            Thread {
                val threadFunction = test.threadFunctions[threadIndex]
                for (i in states.indices) {
                    if (i % syncPeriod == 0) barrier.await()
                    states[i].threadFunction()
                }
            }
        }
        threads.forEach { it.start() }

        return {
            threads.forEach { it.join() }
            val outcomes = states.asSequence().map { it.outcomeFinalizer() }
            outcomes.calcStats(test.outcomeSpec)
        }
    }
}
