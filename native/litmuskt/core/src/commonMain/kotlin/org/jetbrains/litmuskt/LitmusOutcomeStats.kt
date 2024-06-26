package org.jetbrains.litmuskt

typealias LitmusOutcome = Any?

enum class LitmusOutcomeType { ACCEPTED, INTERESTING, FORBIDDEN }

data class LitmusOutcomeStats(
    val outcome: LitmusOutcome,
    val count: Long,
    val type: LitmusOutcomeType?,
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
 * See [LitmusAutoOutcome] file for those extension functions. The generic <S> is used precisely for them.
 *
 * single values are handled differently !!!!!!!!!!!!! TODO
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

typealias LitmusResult = List<LitmusOutcomeStats>

fun LitmusResult.generateTable(): String {
    val totalCount = sumOf { it.count }
    val table = this.sortedByDescending { it.count }.map {
        val freq = it.count.toDouble() / totalCount
        listOf(
            it.outcome.toString(),
            it.type.toString(),
            it.count.toString(),
            if (freq < 1e-5) "<0.001%" else "${(freq * 100).toString().take(6)}%"
        )
    }
    val tableHeader = listOf("outcome", "type", "count", "frequency")
    return (listOf(tableHeader) + table).tableFormat(true)
}

fun List<LitmusResult>.mergeResults(): LitmusResult {
    data class LTOutcomeStatTempData(var count: Long, var type: LitmusOutcomeType?)

    val statMap = mutableMapOf<LitmusOutcome, LTOutcomeStatTempData>()
    for (stat in this.flatten()) {
        val tempData = statMap.getOrPut(stat.outcome) { LTOutcomeStatTempData(0L, stat.type) }
        if (tempData.type != stat.type) error("merging conflicting stats: ${stat.outcome} is both ${stat.type} and ${tempData.type}")
        tempData.count += stat.count
    }
    return statMap.map { (outcome, tempData) -> LitmusOutcomeStats(outcome, tempData.count, tempData.type) }
}
