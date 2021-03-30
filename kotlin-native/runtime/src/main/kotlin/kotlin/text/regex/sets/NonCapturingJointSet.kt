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
 * Node representing non-capturing group
 */
open internal class NonCapturingJointSet(children: List<AbstractSet>, fSet: FSet) : JointSet(children, fSet) {

    /**
     * Returns startIndex+shift, the next position to match
     */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val start = matchResult.getConsumed(groupIndex)
        matchResult.setConsumed(groupIndex, startIndex)

        children.forEach {
            val shift = it.matches(startIndex, testString, matchResult)
            if (shift >= 0) {
                return shift
            }
        }

        matchResult.setConsumed(groupIndex, start)
        return -1
    }

    override val name: String
        get() = "NonCapturingJointSet"

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        return matchResult.getConsumed(groupIndex) != 0
    }
}
