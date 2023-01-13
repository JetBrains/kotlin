/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

/**
 * Reluctant version of the fixed length quantifier set.
 */
internal class ReluctantFixedLengthQuantifierSet(
    quantifier: Quantifier,
    innerSet: AbstractSet,
    next: AbstractSet,
    type: Int
) : FixedLengthQuantifierSet(quantifier, innerSet, next, type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex

        // Process first min occurrences.
        repeat(min) {
            val nextIndex = innerSet.matches(index, testString, matchResult)
            if (nextIndex < 0) {
                return -1
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
                index = nextIndex
                occurrences += 1
            } else {
                return nextIndex
            }
        }

        return next.matches(index, testString, matchResult)
    }
}