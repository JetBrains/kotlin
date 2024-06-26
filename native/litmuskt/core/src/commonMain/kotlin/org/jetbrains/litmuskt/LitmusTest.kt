package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.autooutcomes.LitmusAutoOutcome

data class LitmusTest<S : Any>(
    val stateProducer: () -> S,
    val threadFunctions: List<S.() -> Unit>,
    val outcomeFinalizer: (S.() -> LitmusOutcome),
    val outcomeSpec: LitmusOutcomeSpec
) {
    val threadCount = threadFunctions.size
}

class LitmusTestScope<S : Any>(
    private val stateProducer: () -> S
) {
    private val threadFunctions = mutableListOf<S.() -> Unit>()
    private lateinit var outcomeFinalizer: S.() -> LitmusOutcome
    private lateinit var outcomeSpec: LitmusOutcomeSpecScope<S>

    fun thread(function: S.() -> Unit) {
        threadFunctions.add(function)
    }

    fun outcome(function: S.() -> LitmusOutcome) {
        if (::outcomeFinalizer.isInitialized) error("cannot set outcome more than once")
        outcomeFinalizer = function
    }

    fun spec(setup: LitmusOutcomeSpecScope<S>.() -> Unit) {
        if (::outcomeSpec.isInitialized) error("cannot set spec more than once")
        outcomeSpec = LitmusOutcomeSpecScope<S>().apply(setup)
    }

    fun build(): LitmusTest<S> {
        if (threadFunctions.size < 2) error("tests require at least two threads")
        if (!::outcomeSpec.isInitialized) error("spec not specified")
        val outcomeFinalizer: S.() -> LitmusOutcome = when {
            ::outcomeFinalizer.isInitialized -> outcomeFinalizer
            stateProducer() is LitmusAutoOutcome -> {
                { this }
            }

            else -> error("outcome not specified")
        }
        return LitmusTest(stateProducer, threadFunctions, outcomeFinalizer, outcomeSpec.build())
    }
}

fun <S : Any> litmusTest(stateProducer: () -> S, setup: LitmusTestScope<S>.() -> Unit): LitmusTest<*> =
    LitmusTestScope(stateProducer).apply(setup).build()
