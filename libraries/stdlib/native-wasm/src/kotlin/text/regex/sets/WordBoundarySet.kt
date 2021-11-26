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
 * Represents word boundary, checks current character and previous one. If they have different types returns true;
 */
internal class WordBoundarySet(var positive: Boolean) : SimpleSet() {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val curChar = if (startIndex >= testString.length) ' ' else testString[startIndex]
        val prevChar = if (startIndex == 0) ' ' else testString[startIndex - 1]

        val right = curChar == ' ' || isSpace(curChar, startIndex, testString)
        val left = prevChar == ' ' || isSpace(prevChar, startIndex - 1, testString)

        return if (left xor right xor positive)
            -1
        else
            next.matches(startIndex, testString, matchResult)
    }

    /** Returns false, because word boundary does not consumes any characters and do not move string index. */
    override fun hasConsumed(matchResult: MatchResultImpl): Boolean = false
    override val name: String
        get() = "WordBoundarySet"

    private fun isSpace(char: Char, startIndex: Int, testString: CharSequence): Boolean {
        if (char.isLetterOrDigit() || char == '_') {
            return false
        }
        if (char.category == CharCategory.NON_SPACING_MARK) {
            var index = startIndex
            while (--index >= 0) {
                val ch = testString[index]
                when {
                    ch.isLetterOrDigit() -> return false
                    char.category != CharCategory.NON_SPACING_MARK -> return true
                }
            }
        }
        return true
    }
}



