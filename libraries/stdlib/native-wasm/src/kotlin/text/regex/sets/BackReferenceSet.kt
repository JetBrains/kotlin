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
 * Back reference node;
 */
open internal class BackReferenceSet(val referencedGroup: Int, val consCounter: Int, val ignoreCase: Boolean = false)
    : SimpleSet() {


    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val groupValue = getReferencedGroupValue(matchResult)

        if (groupValue == null || startIndex + groupValue.length > testString.length) {
            return -1
        }

        if (testString.startsWith(groupValue, startIndex, ignoreCase)) {
            matchResult.setConsumed(consCounter, groupValue.length)
            return next.matches(startIndex + groupValue.length, testString, matchResult)
        }
        return -1
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val groupValue = getReferencedGroupValue(matchResult)
        if (groupValue == null || startIndex + groupValue.length > testString.length) {
            return -1
        }

        var index = startIndex
        while (index <= testString.length) {
            index = testString.indexOf(groupValue, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (index < testString.length
                && next.matches(index + groupValue.length, testString, matchResult) >=0) {
                return index
            }
            index++
        }
        return -1
    }

    override fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val groupValue = getReferencedGroupValue(matchResult)
        if (groupValue == null || leftLimit + groupValue.length > rightLimit) {
            return -1
        }

        var index = rightLimit
        while (index >= leftLimit) {
            index = testString.lastIndexOf(groupValue, index, ignoreCase)
            if (index < 0) {
                return -1
            }
            if (index >= 0 && next.matches(index + groupValue.length, testString, matchResult) >= 0) {
                return index
            }
            index--
        }
        return -1
    }


    protected fun getReferencedGroupValue(matchResult: MatchResultImpl) = matchResult.group(referencedGroup)
    override val name: String
            get() = "back reference: $referencedGroup"

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        val result = matchResult.getConsumed(consCounter) != 0
        matchResult.setConsumed(consCounter, -1)
        return result
    }
}
