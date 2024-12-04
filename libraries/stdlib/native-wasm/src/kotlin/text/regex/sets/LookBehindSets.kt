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
 * Positive lookbehind node.
 */
internal class PositiveLookBehindSet(children: List<AbstractSet>, fSet: FSet) : LookAroundSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex)
        children.forEach {
            if (it.findBack(0, startIndex, testString, matchResult) >= 0) {
                matchResult.setConsumed(groupIndex, -1)
                return next.matches(startIndex, testString, matchResult)
            }
        }

         return -1
    }

    override val name: String
        get() = "PositiveBehindJointSet"
}

/**
 * Negative look behind node.
 */
internal class NegativeLookBehindSet(children: List<AbstractSet>, fSet: FSet) : LookAroundSet(children, fSet) {

    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex)

        children.forEach {
            val shift = it.findBack(0, startIndex, testString, matchResult)
            if (shift >= 0) {
                return -1
            }
        }

        return next.matches(startIndex, testString, matchResult)
    }

    override val name: String
        get() = "NegativeBehindJointSet"
}
