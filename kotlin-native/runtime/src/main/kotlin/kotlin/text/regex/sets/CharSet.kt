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
 * Represents node accepting single character.
 */
open internal class CharSet(char: Char, val ignoreCase: Boolean = false) : LeafSet() {

    // We use only low case characters when working in case insensitive mode.
    val char: Char = if (ignoreCase) char.toLowerCase() else char

    // Overrides =======================================================================================================

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        if (ignoreCase) {
            return if (this.char == testString[startIndex].toLowerCase()) 1 else -1
        } else {
            return if (this.char == testString[startIndex]) 1 else -1
        }
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        while (index < testString.length) {
            index = testString.indexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index++
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = rightLimit
        while (index >= leftLimit) {
            index = testString.lastIndexOf(char, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (next.matches(index + charCount, testString, matchResult) >= 0) {
                return index
            }
            index--
        }
        return -1
    }

    override val name: String
            get()= char.toString()

    override fun first(set: AbstractSet): Boolean {
        if (ignoreCase) {
            return super.first(set)
        }
        return when (set) {
            is CharSet -> set.char == char
            is RangeSet -> set.accepts(0, char.toString()) > 0
            is SupplementaryCharSet -> false
            is SupplementaryRangeSet -> set.contains(char)
            else -> true
        }
    }
}