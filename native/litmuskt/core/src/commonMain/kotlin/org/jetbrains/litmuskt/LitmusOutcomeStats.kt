package org.jetbrains.litmuskt

typealias LitmusOutcome = Any?

enum class LitmusOutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class LitmusOutcomeStats(
    val outcome: LitmusOutcome,
    val count: Long,
    val type: LitmusOutcomeType,
)

data class LitmusOutcomeSpec(
    val accepted: Set<LitmusOutcome>,
    val interesting: Set<LitmusOutcome>,
    val forbidden: Set<LitmusOutcome>,
    val default: LitmusOutcomeType,
) {
    fun getType(outcome: LitmusOutcome) = when (outcome) {
        in accepted -> LitmusOutcomeType.ACCEPTED
        in interesting -> LitmusOutcomeType.INTERESTING
        in forbidden -> LitmusOutcomeType.FORBIDDEN
        else -> default
    }

    val all = accepted + interesting + forbidden
}

/**
 * For convenience, it is possible to use `accept(r1, r2, ...)` if outcome is a [LitmusAutoOutcome].
 * In other cases, use `accept(setOf(...))` to accept one or many values. Note that to accept an iterable,
 * it has to be wrapped in an extra `setOf()`. All of this applies as well to `interesting()` and `forbid()`.
 *
 * See [LitmusAutoOutcome] file for `accept(r1, ...)` extension functions. The generic <S> is used precisely for them.
 */
class LitmusOutcomeSpecScope<S : Any> {
    private val accepted = mutableSetOf<LitmusOutcome>()
    private val interesting = mutableSetOf<LitmusOutcome>()
    private val forbidden = mutableSetOf<LitmusOutcome>()
    private var default: LitmusOutcomeType? = null

    // note: if S is LitmusIOutcome, even single values should be interpreted as r1

    fun accept(outcomes: Iterable<LitmusOutcome>) {
        accepted.addAll(outcomes)
    }

    fun interesting(outcomes: Iterable<LitmusOutcome>) {
        interesting.addAll(outcomes)
    }

    fun forbid(outcomes: Iterable<LitmusOutcome>) {
        forbidden.addAll(outcomes)
    }

    fun default(outcomeType: LitmusOutcomeType) {
        if (default != null)
            error("cannot set default outcome type more than once")
        default = outcomeType
    }

    fun build() = LitmusOutcomeSpec(accepted, interesting, forbidden, default ?: LitmusOutcomeType.FORBIDDEN)
}
