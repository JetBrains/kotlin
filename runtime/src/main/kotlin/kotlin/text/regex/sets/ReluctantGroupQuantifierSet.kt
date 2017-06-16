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
 * Reluctant version of the group quantifier set.
 */
internal class ReluctantGroupQuantifierSet(
        quantifier: Quantifier,
        innerSet: AbstractSet,
        next: AbstractSet,
        type: Int,
        setCounter: Int
) : GroupQuantifierSet(quantifier, innerSet, next, type, setCounter) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {

        if (!innerSet.hasConsumed(matchResult)) {
            return next.matches(startIndex, testString, matchResult)
        }

        // Fast case: '*' or {0, } - no need to count occurrences.
        if (min == 0 && max == Quantifier.INF) {
            val res = next.matches(startIndex, testString, matchResult)
            return if (res < 0) {
                innerSet.matches(startIndex, testString, matchResult)
            } else {
                res
            }
        }

        val enterCounter = matchResult.enterCounters[groupQuantifierIndex]

        // can't go inner set;
        if (enterCounter >= max) {
            matchResult.enterCounters[groupQuantifierIndex] = 0
            return next.matches(startIndex, testString, matchResult)
        }

        var nextIndex: Int
        if (enterCounter >= min) {
            nextIndex = next.matches(startIndex, testString, matchResult)
            if (nextIndex < 0) {
                matchResult.enterCounters[groupQuantifierIndex]++
                nextIndex = innerSet.matches(startIndex, testString, matchResult)
            } else {
                matchResult.enterCounters[groupQuantifierIndex] = 0
                return nextIndex
            }
        } else {
            matchResult.enterCounters[groupQuantifierIndex]++
            nextIndex = innerSet.matches(startIndex, testString, matchResult)
        }

        return nextIndex
    }
}
