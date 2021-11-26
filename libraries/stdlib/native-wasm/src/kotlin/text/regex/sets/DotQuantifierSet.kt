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
 * Special node for ".*" construction.
 * The main idea here is to find line terminator and try to find the rest of the construction from this point.
 */
// TODO: Add optimized implementation for '.+' case
internal class DotQuantifierSet(
        innerSet: AbstractSet,
        next: AbstractSet,
        type: Int,
        val lineTerminator: AbstractLineTerminator,
        val matchLineTerminator: Boolean = false
) : QuantifierSet(innerSet, next, type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val rightBound = testString.length
        val startSearch = if (matchLineTerminator) rightBound else testString.findLineTerminator(startIndex, rightBound)

        if (startSearch <= startIndex) {
            return if (type.toChar() == '+') {
                -1
            } else {
                next.matches(startIndex, testString, matchResult)
            }
        }
        val result = next.findBack(startIndex, startSearch, testString, matchResult)
        if (type.toChar() == '+' && result == startIndex) {
            return -1
        }
        return result
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val rightBound = testString.length
        if (matchLineTerminator) {
            val foundIndex = next.findBack(startIndex, rightBound, testString, matchResult)
            if (foundIndex >= 0 && !(type.toChar() == '+' && foundIndex == startIndex)) {
                return startIndex
            } else {
                return -1
            }
        } else {
            // 1. find first occurrence of the searched pattern.
            var nextFound = next.find(startIndex, testString, matchResult)
            if (nextFound < 0) {
                return -1
            }

            // 2. Check if we have other occurrences till the end of line (because .* is greedy and we need the last one).
            val nextFoundLast = next.findBack(nextFound,
                    testString.findLineTerminator(nextFound, rightBound),
                    testString, matchResult)
            nextFound = maxOf(nextFound, nextFoundLast)

            // 3. Find the left boundary of this search.
            val leftBound = findBackLineTerminator(startIndex, nextFound, testString)
            if (type.toChar() == '+' && leftBound + 1 == nextFound) {
                return -1
            }
            return leftBound + 1
        }
    }

    /**
     * Find the first line terminator between [from] (inclusive) and [to] (exclusive) indices.
     * Returns [to] if no terminator found.
     */
    private fun CharSequence.findLineTerminator(from: Int, to: Int): Int =
        (from until to).firstOrNull { lineTerminator.isLineTerminator(this[it]) } ?: to

    /**
     * Find the first line terminator between [from] (inclusive) and [to] (exclusive) indices.
     * Returns [from - 1] if no terminator found.
     */
    private fun findBackLineTerminator(from: Int, to: Int, testString: CharSequence): Int =
        (from until to).lastOrNull { lineTerminator.isLineTerminator(testString[it]) } ?: from - 1

    override val name: String
            get() = ".*"
}
