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
 * Computes the length of a deterministic pattern starting at [this] and terminated by [terminator], in chars.
 *
 * This function only consider [LeafSet]s as deterministic patterns with a known match length, presence
 * of any other sets will result in the whole chain considered nondeterministic.
 *
 * If a chain is nondeterministic, this function returns `-1`.
 */
private fun AbstractSet.computeMatchLengthInChars(terminator: FSet): Int {
    var length = 0
    var set = this
    while (set !== terminator) {
        if (set !is LeafSet) return -1
        length += set.charCount
        set = set.next
    }
    return length
}

internal abstract class LookBehindSetBase(children: List<AbstractSet>, fSet: FSet) : LookAroundSet(children, fSet) {
    // For leaf sets, we only have to scan a fixed-length prefix of the input;
    // this array contains the length of this prefix, or -1 if it is unknown.
    val prefixLengths = IntArray(children.size) {
        children[it].computeMatchLengthInChars(fSet)
    }

    protected fun matchPrefix(
        childIndex: Int,
        child: AbstractSet,
        startIndex: Int,
        testString: CharSequence,
        matchResult: MatchResultImpl
    ): Int {
        val prefixLength = prefixLengths[childIndex]
        return when {
            // the prefix length is unknown, so lets fallback to a generic matching
            prefixLength < 0 -> child.findBack(0, startIndex, testString, matchResult)
            // the pattern has a known length, but the prefix is shorter, so the regular expression will never match
            startIndex - prefixLength < 0 -> -1
            // the pattern has a known length and it fits the into input's prefix, so let's test it!
            else -> child.matches(startIndex - prefixLength, testString, matchResult)
        }
    }
}

/**
 * Positive lookbehind node.
 */
internal class PositiveLookBehindSet(children: List<AbstractSet>, fSet: FSet) : LookBehindSetBase(children, fSet) {
    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex)
        forEachChildrenIndexed { idx, child ->
            if (matchPrefix(idx, child, startIndex, testString, matchResult) >= 0) {
                matchResult.setConsumed(groupIndex, -1)
                return next.matches(startIndex, testString, matchResult)
            }
        }

        return -1
    }

    override val name: String
        get() = "PositiveBehindJointSet"

    override fun reportOwnProperties(properties: SetProperties) {
        forEachChildrenIndexed { _, child -> child.collectProperties(properties, fSet) }
        properties.nonTrivialBacktracking = true // just in case
        properties.requiresCheckpointing = true
        properties.tracksConsumption = true
    }
}

/**
 * Negative look behind node.
 */
internal class NegativeLookBehindSet(children: List<AbstractSet>, fSet: FSet) : LookBehindSetBase(children, fSet) {
    /** Returns startIndex+shift, the next position to match */
    override fun tryToMatch(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        matchResult.setConsumed(groupIndex, startIndex)
        forEachChildrenIndexed { idx, child ->
            if (matchPrefix(idx, child, startIndex, testString, matchResult) >= 0) {
                return -1
            }
        }

        return next.matches(startIndex, testString, matchResult)
    }

    override val name: String
        get() = "NegativeBehindJointSet"

    override fun reportOwnProperties(properties: SetProperties) {
        forEachChildrenIndexed { _, child -> child.collectProperties(properties, fSet) }
        properties.nonTrivialBacktracking = true // just in case
        properties.requiresCheckpointing = true
        properties.tracksConsumption = true
    }
}
