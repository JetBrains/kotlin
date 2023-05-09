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
 * Node accepting any character except line terminators.
 */
internal class DotSet(val lt: AbstractLineTerminator, val matchLineTerminator: Boolean)
    : SimpleSet(AbstractSet.TYPE_DOTSET) {

    // This node consumes any character. If the character is supplementary, this node consumes both surrogate chars representing it.
    // Otherwise, consumes a single char.
    // Thus, for a given [testString] and [startIndex] a fixed amount of chars are consumed.
    override val consumesFixedLength: Boolean
        get() = true

    @OptIn(ExperimentalNativeApi::class)
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val rightBound = testString.length
        if (startIndex >= rightBound) {
            return -1
        }

        val high = testString[startIndex]
        if (high.isHighSurrogate() && startIndex + 2 <= rightBound) {
            val low = testString[startIndex + 1]
            if (Char.isSurrogatePair(high, low)) {
                if (!matchLineTerminator && lt.isLineTerminator(Char.toCodePoint(high, low))) {
                    return -1
                } else {
                    return next.matches(startIndex + 2, testString, matchResult)
                }
            }
        }
        if (!matchLineTerminator && lt.isLineTerminator(high)) {
            return -1
        } else {
            return next.matches(startIndex + 1, testString, matchResult)
        }
    }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean = true
    override val name: String
        get() = "."
}
