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
 * A node representing a '^' sign.
 * Note: In Kotlin we use only the "anchoring bounds" mode when "^" matches beginning of a match region.
 * See: http://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html#useAnchoringBounds-boolean-
 */
internal class SOLSet(val lt: AbstractLineTerminator, val multiline: Boolean = false) : SimpleSet() {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        if (!multiline) {
            if (startIndex == 0) {
                return next.matches(startIndex, testString, matchResult)
            }
        } else {
            if (startIndex != testString.length
                && (startIndex == 0
                    || lt.isAfterLineTerminator(testString[startIndex - 1], testString[startIndex]))) {
                return next.matches(startIndex, testString, matchResult)
            }
        }
        return -1
    }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean = false
    override val name: String
        get() = "^"
}