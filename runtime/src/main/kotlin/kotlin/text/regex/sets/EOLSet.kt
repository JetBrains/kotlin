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
 * Represents a node for a '$' sign.
 * Note: In Kotlin we use only the "anchoring bounds" mode when "$" matches the end of a match region.
 * See: http://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html#useAnchoringBounds-boolean-
 */
internal class EOLSet(val consCounter: Int, val lt: AbstractLineTerminator, val multiline: Boolean = false) : SimpleSet() {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val rightBound = testString.length
        val remainingChars = rightBound - startIndex

        when {
            startIndex >= rightBound ||
            remainingChars == 1 && lt.isLineTerminator(testString[startIndex]) ||
            remainingChars == 2 && lt.isLineTerminatorPair(testString[startIndex], testString[startIndex+1]) ||
            multiline && lt.isLineTerminator(testString[startIndex]) -> {
                matchResult.setConsumed(consCounter, 0)
                return next.matches(startIndex, testString, matchResult)
            }
        }
        return -1
    }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        val result = matchResult.getConsumed(consCounter) != 0
        matchResult.setConsumed(consCounter, -1)
        return result
    }

    override val name: String
            get()= "<EOL>"
}
