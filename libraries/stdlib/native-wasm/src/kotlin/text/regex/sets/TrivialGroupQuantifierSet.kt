/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

/**
 * Greedy quantifier over a trivial capturing group or non capturing group.
 *
 * Trivial, here, means that:
 * - we don't have to reevaluate a group in different ways when a match failed (i.e. we don't have to backtrack)
 * - the group does not contain any nested capturing groups
 * - there is no state checkpointing during evaluation (specifics of lookaround groups)
 *
 * Such "trivial" groups could be evaluated without a recursion. This set is a gemini of [FixedLengthQuantifierSet],
 * but tweaked to work with groups.
 *
 * For non-trivial group, use [GroupQuantifierSet] instead (which supports proper backtracking at the cost of recursion).
 */
internal class TrivialGroupQuantifierSet private constructor(
    val quantifier: Quantifier,
    innerSet: JointSet,
    next: AbstractSet,
    type: Int
) : QuantifierSet(innerSet, next, type) {
    val groupIdx: Int

    init {
        innerSet.next = FSet.possessiveFSet
        groupIdx = when (innerSet) {
            is SingleSet -> innerSet.groupIndex
            is NonCapturingJointSet -> -1
            else -> error("Unexpected inner set type: $innerSet")
        }
    }

    val min: Int get() = quantifier.min
    val max: Int get() = quantifier.max

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        val matches = mutableListOf<Int>()
        val groups = mutableListOf<Pair<Int, Int>>()

        // Process occurrences between 0 and max.
        while (max == Quantifier.INF || matches.size < max) {
            val nextIndex = innerSet.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                if (matches.size < min) {
                    return -1
                } else {
                    break
                }
            }
            matches.add(index)
            if (groupIdx != -1) {
                // For capturing groups (groupIdx != -1, and) consequent successful matches override
                // group boundaries. This set handles only trivial groups that don't contain any nested groups.
                // So the only group we should care and the only group, whose boundaries we have to restore
                // when rolling back is a group with groupIdx.
                groups.add(matchResult.getStart(groupIdx) to matchResult.getEnd(groupIdx))
            }
            if (!innerSet.hasConsumed(matchResult)) {
                // It does not make sense to continue: the inner set consumed nothing (but matched), and that means that
                // the matched substring is empty.
                // Since it is "trivial", it will match the same empty substring infinitely many times
                // (which is not a good idea, we want to stop at some point).
                // If a minimal number of matches was already achieved, we can exit the loop and match the next sub-pattern.
                // If not - it does not matter, as we will match the same substring once again, so why bother matching it over and over?
                // So let's stop.
                break
            }
            index = nextIndex
        }

        // Roll back if the next node doesn't match the remaining string.
        while (matches.size > min) {
            val nextIndex = next.matches(index, testString, matchResult)
            if (nextIndex >= 0) {
                return nextIndex
            }
            index = matches.removeLast()
            if (groupIdx != -1) {
                // We're handling a capturing group, let's restore its boundaries to a previous match.
                val (groupStartIndex, groupEndIndex) = groups.removeLast()
                matchResult.setStart(groupIdx, groupStartIndex)
                matchResult.setEnd(groupIdx, groupEndIndex)
            }
        }

        return next.matches(index, testString, matchResult)
    }

    override fun toString(): String {
        return "${this::class}(innerSet = $innerSet, next = $next)"
    }

    override fun reportOwnProperties(properties: SetProperties) {
        innerSet.collectProperties(properties)
        properties.nonTrivialBacktracking = true // TODO: maybe we can relax the it in certain cases?
    }

    internal companion object {
        fun constructIfInnerSetQualifiesOrNull(
            quantifier: Quantifier,
            innerSet: AbstractSet,
            next: AbstractSet,
            type: Int
        ): TrivialGroupQuantifierSet? {
            if (innerSet !is JointSet || !innerSet.isTrivialGroupForQuantification()) {
                return null
            }
            return TrivialGroupQuantifierSet(quantifier, innerSet, next, type)
        }
    }
}
