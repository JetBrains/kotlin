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
 * The node which marks end of the particular group.
 */
open internal class FSet(val groupIndex: Int) : SimpleSet() {

    var isBackReferenced = false

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val oldEnd = matchResult.getEnd(groupIndex)
        matchResult.setEnd(groupIndex, startIndex)
        val shift = next.matches(startIndex, testString, matchResult)
        if (shift < 0) {
            matchResult.setEnd(groupIndex, oldEnd)
        }
        return shift
    }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean = false
    override val name: String
            get() = "fSet"

    override fun processSecondPass(): FSet {
        val result = super.processSecondPass()
        assert(result == this)
        return this
    }

    /**
     * Marks the end of the particular group and not take into account possible
     * kickbacks (required for atomic groups, for instance)
     */
    internal class PossessiveFSet : SimpleSet() {
        override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
            return startIndex
        }

        override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
            return false
        }

        override val name: String
                get() = "possessiveFSet"
    }

    companion object {
        val possessiveFSet = PossessiveFSet()
    }
}

/**
 * Special construction which marks end of pattern.
 */
internal class FinalSet : FSet(0) {

    override fun matches(startIndex: Int, testString: CharSequence,
                         matchResult: MatchResultImpl): Int {
        if (matchResult.mode == Regex.Mode.FIND || startIndex == testString.length) {
            matchResult.setEnd(0, startIndex)
            return startIndex
        }
        return -1
    }

    override val name: String
        get() = "FinalSet"
}

/**
 * Non-capturing group closing node.
 */
internal class NonCapFSet(groupIndex: Int) : FSet(groupIndex) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex - matchResult.getConsumed(groupIndex))
        return next.matches(startIndex, testString, matchResult)
    }

    override val name: String
        get() = "NonCapFSet"

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        return false
    }
}

/**
 * LookAhead FSet, always returns true
 */
internal class AheadFSet : FSet(-1) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        return startIndex
    }

    override val name: String
        get() = "AheadFSet"
}

/**
 * FSet for lookbehind constructs. Checks if string index saved by corresponding
 * jointSet in "consumers" equals to current index and return current string
 * index, return -1 otherwise.

 */
internal class BehindFSet(groupIndex: Int) : FSet(groupIndex) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        val rightBound = matchResult.getConsumed(groupIndex)
        return if (rightBound == startIndex) startIndex else -1
    }

    override val name: String
        get() = "BehindFSet"
}

/**
 * Represents an end of an atomic group.
 */
internal class AtomicFSet(groupIndex: Int) : FSet(groupIndex) {

    var index: Int = 0

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex - matchResult.getConsumed(groupIndex))
        index = startIndex
        return startIndex
    }

    override val name: String
        get() = "AtomicFSet"

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        return false
    }
}
