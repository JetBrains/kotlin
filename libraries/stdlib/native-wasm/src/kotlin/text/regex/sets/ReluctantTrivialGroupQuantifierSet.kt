/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

// TODO: merge this class and ReluctantFixedLengthQuantifierSet
/**
 * Reluctant quantifier over a trivial capturing group or non capturing group.
 *
 * Trivial, here, means that:
 * - we don't have to reevaluate a group in different ways when a match failed (i.e. we don't have to backtrack)
 * - the group does not contain any nested capturing groups
 * - there is no state checkpointing during evaluation (specifics of lookaround groups)
 *
 * Such "trivial" groups could be evaluated without a recursion. This set is a gemini of [ReluctantFixedLengthQuantifierSet],
 * but tweaked to work with groups.
 *
 * For non-trivial group, use [ReluctantGroupQuantifierSet] instead (which supports proper backtracking at the cost of recursion).
 */
internal class ReluctantTrivialGroupQuantifierSet private constructor(
    val quantifier: Quantifier,
    innerSet: AbstractSet,
    next: AbstractSet,
    type: Int
) : QuantifierSet(innerSet, next, type) {

    init {
        innerSet.next = FSet.possessiveFSet
    }

    val min: Int get() = quantifier.min
    val max: Int get() = quantifier.max

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex

        // Process first min occurrences.
        repeat(min) {
            val nextIndex = innerSet.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                return -1
            }
            if (!innerSet.hasConsumed(matchResult)) {
                // It does not make sense to continue, inner set will repeatedly match an empty substring
                return@repeat
            }
            index = nextIndex
        }

        // Process occurrences between min and max.
        var occurrences = min
        while (max == Quantifier.INF || occurrences < max) {
            var nextIndex = next.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                nextIndex = innerSet.matches(index, testString, matchResult)
                if (nextIndex < 0) {
                    return -1
                }
                if (!innerSet.hasConsumed(matchResult)) {
                    // It's nice that innerSet matched,
                    // but since it didn't consume input (i.e., it matched an empty string),
                    // the next will never match anyway
                    return -1
                }
                index = nextIndex
                occurrences += 1
            } else {
                return nextIndex
            }
        }

        return next.matches(index, testString, matchResult)
    }

    override fun reportOwnProperties(properties: SetProperties) {
        innerSet.reportOwnProperties(properties)
        properties.nonTrivialBacktracking = true
    }

    companion object {
        fun constructIfInnerSetQualifiesOrNull(
            quantifier: Quantifier,
            innerSet: AbstractSet,
            next: AbstractSet,
            type: Int
        ): ReluctantTrivialGroupQuantifierSet? {
            if (innerSet !is JointSet || !innerSet.isTrivialGroupForQuantification()) {
                return null
            }
            return ReluctantTrivialGroupQuantifierSet(quantifier, innerSet, next, type)
        }
    }
}
