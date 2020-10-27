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
 * Match result implementation
 */

internal class MatchResultImpl
/**
 *  @param input an input sequence for matching/searching.
 *  @param regex a [Regex] instance used for matching/searching.
 *  @param rightBound index in the [input] used as a right bound for matching/searching. Exclusive.
 */
constructor (internal val input: CharSequence,
             internal val regex: Regex) : MatchResult {

    // Harmony's implementation ========================================================================================
    private val nativePattern = regex.nativePattern
    private val groupCount = nativePattern.capturingGroupCount
    private val groupBounds = IntArray(groupCount * 2) { -1 }

    private val consumers = IntArray(nativePattern.consumersCount + 1) { -1 }

    // Used by quantifiers to store a count of a quantified expression occurrences.
    val enterCounters: IntArray = IntArray( maxOf(nativePattern.groupQuantifierCount, 0) )

    var startIndex: Int = 0
        set (startIndex: Int) {
            field = startIndex
            if (previousMatch < 0) {
                previousMatch = startIndex
            }
        }

    var previousMatch = -1
    var mode = Regex.Mode.MATCH

    // MatchResult interface ===========================================================================================
    /** The range of indices in the original string where match was captured. */
    override val range: IntRange
        get() = getStart(0) until getEnd(0)

    /** The substring from the input string captured by this match. */
    override val value: String
        get() = group(0) ?: throw AssertionError("No groupIndex #0 in the match result.")

    /**
     * A collection of groups matched by the regular expression.
     *
     * This collection has size of `groupCount + 1` where `groupCount` is the count of groups in the regular expression.
     * Groups are indexed from 1 to `groupCount` and group with the index 0 corresponds to the entire match.
     */
    // Create one object or several ones?
    override val groups: MatchGroupCollection = object: MatchGroupCollection, AbstractCollection<MatchGroup?>() {
        override val size: Int
            get() = this@MatchResultImpl.groupCount


        override fun iterator(): Iterator<MatchGroup?> {
            return object: Iterator<MatchGroup?> {
                var nextIndex: Int = 0

                override fun hasNext(): Boolean {
                    return nextIndex < size
                }

                override fun next(): MatchGroup? {
                    if (!hasNext()) {
                        throw NoSuchElementException()
                    }
                    return get(nextIndex++)
                }
            }
        }

        override fun get(index: Int): MatchGroup? {
            val value = group(index) ?: return null
            return MatchGroup(value, getStart(index) until getEnd(index))
        }
    }

    /**
     * A list of matched indexed group values.
     *
     * This list has size of `groupCount + 1` where `groupCount` is the count of groups in the regular expression.
     * Groups are indexed from 1 to `groupCount` and group with the index 0 corresponds to the entire match.
     *
     * If the group in the regular expression is optional and there were no match captured by that group,
     * corresponding item in [groupValues] is an empty string.
     *
     * @sample: samples.text.Regexps.matchDestructuringToGroupValues
     */
    override val groupValues: List<String>
        get() = mutableListOf<String>().apply {
            for (i in 0 until groupCount) {
                this.add(group(i) ?: "")
            }
        }

    override fun next(): MatchResult? {
        var nextStart = range.endInclusive + 1
        // If the current match is empty - shift by 1.
        if (nextStart == range.start) {
            nextStart++
        }
        if (nextStart > input.length) {
            return null
        }
        return regex.find(input, nextStart)
    }
    // =================================================================================================================


    // Harmony's implementation ========================================================================================
    fun setConsumed(counter: Int, value: Int) {
        this.consumers[counter] = value
    }

    fun getConsumed(counter: Int): Int {
        return this.consumers[counter]
    }

    fun isCaptured(group: Int): Boolean = getStart(group) >= 0

    // Setters and getters for starts and ends of groups ===============================================================
    internal fun setStart(group: Int, offset: Int) {
        checkGroup(group)
        groupBounds[group * 2] = offset
    }

    internal fun setEnd(group: Int, offset: Int) {
        checkGroup(group)
        groupBounds[group * 2 + 1] = offset
    }

    /**
     * Returns the index of the first character of the text that matched a given group.
     *
     * @param group the group, ranging from 0 to groupCount() - 1, with 0 representing the whole pattern.
     * @return the character index.
     */
    fun getStart(group: Int = 0): Int {
        checkGroup(group)
        return groupBounds[group * 2]
    }

    /**
     * Returns the index of the first character following the text that matched a given group.
     *
     * @param group the group, ranging from 0 to groupCount() - 1, with 0 representing the whole pattern.
     * @return the character index.
     */
    fun getEnd(group: Int = 0): Int {
        checkGroup(group)
        return groupBounds[group * 2 + 1]
    }

    // ==================================================================================================

    /**
     * Returns the text that matched a given group of the regular expression.
     *
     * @param group the group, ranging from 0 to groupCount() - 1, with 0 representing the whole pattern.
     * @return the text that matched the group.
     */
    fun group(group: Int = 0): String? {
        val start = getStart(group)
        val end = getEnd(group)
        if (start < 0 || end < 0) {
            return null
        }
        return input.subSequence(getStart(group), getEnd(group)).toString()
    }

    /**
     * Returns the number of groups in the result, which is always equal to
     * the number of groups in the original regular expression.
     *
     * @return the number of groups.
     */
    fun groupCount(): Int {
        return groupCount - 1
    }

    /*
     * This method being called after any successful match; For now it's being
     * used to check zero group for empty match;
     */
    fun finalizeMatch() {
        if (this.groupBounds[0] == -1) {
            this.groupBounds[0] = this.startIndex
            this.groupBounds[1] = this.startIndex
        }

        previousMatch = getEnd()
    }

    private fun checkGroup(group: Int) {
        if (group < 0 || group > groupCount) {
            throw IndexOutOfBoundsException("Group index out of bounds: $group")
        }
    }

    fun updateGroup(index: Int, srtOffset: Int, endOffset: Int) {
        checkGroup(index)
        groupBounds[index * 2] = srtOffset
        groupBounds[index * 2 + 1] = endOffset
    }
}

