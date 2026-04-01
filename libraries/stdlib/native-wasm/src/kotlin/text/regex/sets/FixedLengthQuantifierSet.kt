/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

/**
 * Greedy quantifier over constructions that consume a fixed number of characters.
 */
open internal class FixedLengthQuantifierSet(
    val quantifier: Quantifier,
    innerSet: AbstractSet,
    next: AbstractSet,
    type: Int
) : QuantifierSet(innerSet, next, type) {

    init {
        require(innerSet.consumesFixedLength)
        innerSet.next = FSet.possessiveFSet
    }

    val min: Int get() = quantifier.min
    val max: Int get() = quantifier.max

    override val consumesFixedLength: Boolean
        get() = (min == max)

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        val matches = mutableListOf<Int>()

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
            index = nextIndex
        }

        // Roll back if the next node doesn't match the remaining string.
        while (matches.size > min) {
            val nextIndex = next.matches(index, testString, matchResult)
            if (nextIndex >= 0) {
                return nextIndex
            }
            index = matches.removeLast()
        }

        return next.matches(index, testString, matchResult)
    }

    override fun toString(): String {
        return "${this::class}(innerSet = $innerSet, next = $next)"
    }

    override fun reportOwnProperties(properties: SetProperties) {
        innerSet.collectProperties(properties)
        // if the repetition number is varying or is infinite, we have to backtrack
        properties.nonTrivialBacktracking = properties.nonTrivialBacktracking || min != max || max == Quantifier.INF
    }
}
