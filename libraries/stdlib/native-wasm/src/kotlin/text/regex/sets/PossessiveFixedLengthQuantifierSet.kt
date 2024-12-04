/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

/**
 * Possessive version of the fixed length quantifier set.
 */
internal class PossessiveFixedLengthQuantifierSet(
    quantifier: Quantifier,
    innerSet: AbstractSet,
    next: AbstractSet,
    type: Int
) : FixedLengthQuantifierSet(quantifier, innerSet, next, type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex

        // Process occurrences between 0 and max.
        var occurrences = 0
        while (max == Quantifier.INF || occurrences < max) {
            val nextIndex = innerSet.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                break
            }
            index = nextIndex
            occurrences += 1
        }

        return if (occurrences < min) -1 else next.matches(index, testString, matchResult)
    }
}