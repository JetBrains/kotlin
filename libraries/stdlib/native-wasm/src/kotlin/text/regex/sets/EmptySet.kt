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
 * Valid constant zero character match.
 */
internal class EmptySet(override var next: AbstractSet) : LeafSet() {

    override val charCount = 0

    override fun accepts(startIndex: Int, testString: CharSequence): Int = 0

    override fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        for (index in startIndex..testString.length) {
            if (next.matches(index, testString, matchResult) >= 0) {
                return index
            }
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        for (index in rightLimit downTo leftLimit) {
            if (next.matches(index, testString, matchResult) >= 0) {
                return index
            }
        }
        return -1
    }


    override val name: String
            get()= "<Empty set>"

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        return false
    }

}
