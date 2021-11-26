/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlin.text.regex

/**
 * Default quantifier over groups, in fact this type of quantifier is
 * generally used for constructions we cant identify number of characters they
 * consume.

 */
open internal class GroupQuantifierSet(
        val quantifier: Quantifier,
        innerSet: AbstractSet,
        next: AbstractSet,
        type: Int,
        val groupQuantifierIndex: Int // It's used to remember a number of the innerSet occurrences during the recursive search.
) : QuantifierSet(innerSet, next, type) {

    val max: Int get() = quantifier.max
    val min: Int get() = quantifier.min

    // We call innerSet.matches here, if it succeeds it call next.matches where next is this QuantifierSet.
    // So we have a recursive searching procedure.
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var enterCount = matchResult.enterCounters[groupQuantifierIndex]

        fun matchNext(): Int {
            matchResult.enterCounters[groupQuantifierIndex] = 0
            val result = next.matches(startIndex, testString, matchResult)
            matchResult.enterCounters[groupQuantifierIndex] = enterCount
            return result
        }

        if (!innerSet.hasConsumed(matchResult)) {
            return matchNext()
        }

        // Fast case: '*' or {0, } - no need to count occurrences.
        if (min == 0 && max == Quantifier.INF) {
            val nextIndex = innerSet.matches(startIndex, testString, matchResult)
            return if (nextIndex < 0) {
                matchNext()
            } else {
                nextIndex
            }
        }

        // can't go inner set;
        if (max != Quantifier.INF && enterCount >= max) {
            return matchNext()
        }

        // go inner set;
        matchResult.enterCounters[groupQuantifierIndex] = ++enterCount
        val nextIndex = innerSet.matches(startIndex, testString, matchResult)

        return if (nextIndex < 0) {
            matchResult.enterCounters[groupQuantifierIndex] = --enterCount
            if (enterCount >= min) {
                matchNext()
            } else {
                -1
            }
        } else {
            nextIndex
        }

    }

    override val name: String
            get() = quantifier.toString()
}
