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

import kotlin.experimental.ExperimentalNativeApi

/**
 * This class represents nodes constructed with character sequences. For
 * example, lets consider regular expression: ".*word.*". During regular
 * expression compilation phase character sequence w-o-r-d, will be represented
 * with single node for the entire word.
 */
open internal class SequenceSet(substring: CharSequence, val ignoreCase: Boolean = false) : LeafSet() {

    /** Represents a character sequence used for matching/searching. */
    protected val patternString: String = substring.toString()

    override val name: String= "sequence: " + patternString

    override val charCount = substring.length

    // Overrides =======================================================================================================

    /** Returns true if [index] points to a low surrogate following a high surrogate */
    private fun isLowSurrogateOfSupplement(string: CharSequence, index: Int): Boolean =
        index < string.length && string[index].isLowSurrogate() && index > 0 && string[index - 1].isHighSurrogate()

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        return if (testString.startsWith(patternString, startIndex, ignoreCase)
                    && !isLowSurrogateOfSupplement(testString, startIndex)
                    && !isLowSurrogateOfSupplement(testString, startIndex + patternString.length)) {
            charCount
        } else {
            -1
        }
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        while (index < testString.length) {
            index = testString.indexOf(patternString, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            // Check if we have a supplementary code point at the beginning or at the end of the string.
            if (!isLowSurrogateOfSupplement(testString, index)
                && !isLowSurrogateOfSupplement(testString, index + patternString.length)
                && next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index++
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = rightLimit
        while (index >= leftLimit) {
            index = testString.lastIndexOf(patternString, index, ignoreCase)
            if (index < 0 || index < leftLimit) {
                return -1
            }
            // Check if we have a supplementary code point at the beginning or at the end of the string.
            if (!isLowSurrogateOfSupplement(testString, index)
                    && !isLowSurrogateOfSupplement(testString, index + patternString.length)
                    && next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index--
        }
        return -1
    }

    @OptIn(ExperimentalNativeApi::class)
    override fun first(set: AbstractSet): Boolean {
        if (ignoreCase) {
            return super.first(set)
        }
        return when (set) {
            is CharSet -> set.char == patternString[0]
            is RangeSet -> set.accepts(0, patternString.substring(0, 1)) > 0
            is SupplementaryRangeSet -> set.contains(patternString[0]) || patternString.length > 1 && set.contains(Char.toCodePoint(patternString[0], patternString[1]))
            else -> true
        }
    }
}
