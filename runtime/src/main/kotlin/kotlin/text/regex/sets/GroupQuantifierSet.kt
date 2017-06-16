/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        if (!innerSet.hasConsumed(matchResult)) {
            return next.matches(startIndex, testString, matchResult)
        }

        // Fast case: '*' or {0, } - no need to count occurrences.
        if (min == 0 && max == Quantifier.INF) {
            val nextIndex = innerSet.matches(startIndex, testString, matchResult)

            return if (nextIndex < 0) {
                next.matches(startIndex, testString, matchResult)
            } else {
                nextIndex
            }
        }

        val enterCount = matchResult.enterCounters[groupQuantifierIndex]

        // can't go inner set;
        if (max != Quantifier.INF && enterCount >= max) {
            return next.matches(startIndex, testString, matchResult)
        }

        // go inner set;
        matchResult.enterCounters[groupQuantifierIndex]++
        val nextIndex = innerSet.matches(startIndex, testString, matchResult)

        if (nextIndex < 0) {
            matchResult.enterCounters[groupQuantifierIndex]--
            if (enterCount >= min) {
                return next.matches(startIndex, testString, matchResult)
            } else {
                matchResult.enterCounters[groupQuantifierIndex] = 0
                return -1
            }
        } else {
            matchResult.enterCounters[groupQuantifierIndex] = 0
            return nextIndex
        }

    }

    override val name: String
            get() = quantifier.toString()
}
