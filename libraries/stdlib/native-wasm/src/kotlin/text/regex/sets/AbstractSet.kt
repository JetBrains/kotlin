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

import kotlin.AssertionError

/** Basic class for sets which have no complex next node handling. */
internal abstract class SimpleSet : AbstractSet {
    override var next: AbstractSet = dummyNext
    constructor()
    constructor(type : Int): super(type)
}

/**
 * Basic class for nodes, representing given regular expression.
 * Note: (Almost) All the classes representing nodes has 'set' suffix.
 */
internal abstract class AbstractSet(val type: Int = 0) {

    companion object {
        const val TYPE_LEAF = 1 shl 0
        const val TYPE_FSET = 1 shl 1
        const val TYPE_QUANT = 1 shl 3
        @Suppress("DEPRECATION")
        const val TYPE_DOTSET = 0x80000000.toInt() or '.'.toInt()

        val dummyNext = object : AbstractSet() {
            override var next: AbstractSet
                get() = throw AssertionError("This method is not expected to be called.")
                @Suppress("UNUSED_PARAMETER")
                set(value) {}
            override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl) =
                throw AssertionError("This method is not expected to be called.")
            override fun hasConsumed(matchResult: MatchResultImpl): Boolean =
                throw AssertionError("This method is not expected to be called.")
            override fun processSecondPassInternal(): AbstractSet = this
            override fun processSecondPass(): AbstractSet = this
            override fun reportOwnProperties(properties: SetProperties) {
                // nothing to report
            }

            override fun collectProperties(properties: SetProperties, finalSet: AbstractSet?) {
                //throw AssertionError("This method is not expected to be called.")
            }
        }
    }

    var secondPassVisited = false
    abstract var next: AbstractSet

    protected open val name: String
        get() = ""

    /**
     * Checks if this node matches in given position and recursively call
     * next node matches on positive self match. Returns positive integer if
     * entire match succeed, negative otherwise.
     * @param startIndex - string index to start from.
     * @param testString  - input string.
     * @param matchResult - MatchResult to sore result into.
     * @return -1 if match fails or n > 0;
     */
    abstract fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int

    /**
     * Attempts to apply pattern starting from this set/startIndex; returns
     * index this search was started from, if value is negative, this means that
     * this search didn't succeed, additional information could be obtained via
     * matchResult.
     *
     * Note: this is default implementation for find method, it's based on
     * matches, subclasses do not have to override find method unless
     * more effective find method exists for a particular node type
     * (sequence, i.e. substring, for example). Same applies for find back
     * method.
     *
     * @param startIndex - starting index.
     * @param testString - string to search in.
     * @param matchResult - result of the match.
     * @return last searched index.
     */
    open fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        for (index in startIndex..testString.length) {
            if (matches(index, testString, matchResult) >= 0) {
                return index
            }
        }
        return -1
    }

    /**
     * @param leftLimit - an index, to finish search back (left limit).
     * @param rightLimit - an index to start search from (right limit).
     * @param testString - test string.
     * @param matchResult - match result.
     * @return an index to start back search next time if this search fails(new left bound);
     *         if this search fails the value is negative.
     */
    open fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        for (index in rightLimit downTo leftLimit) {
            if (matches(index, testString, matchResult) >= 0) {
                return index
            }
        }
        return -1
    }

    /**
     * Returns `true` if this node consumes a constant number of characters and doesn't need backtracking to find a different match.
     * Otherwise, returns `false`.
     *
     * This information is used to avoid recursion when matching a quantifier node with this inner node.
     */
    open val consumesFixedLength: Boolean
        get() = false

    /**
     * Returns true, if this node has consumed any characters during
     * positive match attempt, for example node representing character always
     * consumes one character if it matches. If particular node matches
     * empty sting this method will return false.
     *
     * @param matchResult - match result;
     * @return true if the node consumes any character and false otherwise.
     */
    abstract fun hasConsumed(matchResult: MatchResultImpl): Boolean

    /**
     * Returns true if the given node intersects with this one, false otherwise.
     * This method is being used for quantifiers construction, lets consider the
     * following regular expression (a|b)*ccc. (a|b) does not intersect with "ccc"
     * and thus can be quantified greedily (w/o kickbacks), like *+ instead of *.

     * @param set - A node the intersection is checked for. Usually a previous node.
     * @return true if the given node intersects with this one, false otherwise.
     */
    open fun first(set: AbstractSet): Boolean = true

    /**
     * This method is used for replacement backreferenced sets.
     *
     * @return null if current node need not to be replaced,
     *         [JointSet] which is replacement of current node otherwise.
     */
    open fun processBackRefReplacement(): JointSet? {
        return null
    }

    /**
     * This method performs the second pass without checking if it's already performed or not.
     */
    protected open fun processSecondPassInternal(): AbstractSet {
        if (!next.secondPassVisited) {
            this.next = next.processSecondPass()
        }
        return processBackRefReplacement() ?: this
    }

    /**
     * This method is used for traversing nodes after the first stage of compilation.
     */
    open fun processSecondPass(): AbstractSet {
        secondPassVisited = true
        return processSecondPassInternal()
    }

    /**
     * Updates [properties] with information about a tree representing a regular expression
     * rooted at [this] set.
     *
     * Information is collected starting for [this] set until either some final set not having a [next] element
     * is reached, or until the set currently being inspected is not equal to [finalSet].
     * The latter condition is checked for a case when the tree is no longer a tree, but rather a graph with a cycle.
     * That's a particular case for [GroupQuantifierSet], which has a subtree with a final set referencing back to the [GroupQuantifierSet].
     * Unless you don't have a value to supply as a [finalSet], you don't have to worry about it, but if a set has
     * some inner set and specific final set, then the latter could be used as an iteration terminator.
     *
     * This function relies on [reportOwnProperties] to collect properties for this set.
     */
    open fun collectProperties(properties: SetProperties, finalSet: AbstractSet? = null) {
        reportOwnProperties(properties)
        if (next !== finalSet) {
            next.collectProperties(properties, finalSet)
        }
    }

    /**
     * Updates [properties] with information about this set (and its internal set, not reachable via [next]).
     */
    abstract fun reportOwnProperties(properties: SetProperties)
}

/**
 * Various properties affecting how a regular expression might be evaluated.
 *
 * @property capturesGroups if `true`, a regex captures group values when evaluated
 * @property tracksConsumption if `true`, a regex updates [MatchResultImpl.consumers],
 *   which is required to bound [GroupQuantifierSet]'s recursion in certain cases
 * @property nonTrivialBacktracking if `true`, a regex evaluation requires non-trivial backtracking,
 *   mainly because unsuccessful matching, followed by a backtracking may result in evaluating some of the sets
 *   differently. For example, for a RE like `(aa|a)a` and input `"aa"`, a first attempt to match with `(aa)a` will fail,
 *   we will backtrack and re-evaluate the group, to match only a single letter.
 * @property requiresCheckpointing if `true`, regex's evaluation require [MatchResultImpl] checkpointing via
 *   [MatchResultImpl.saveState] and [MatchResultImpl.rollbackState].
 */
internal data class SetProperties(
    var capturesGroups: Boolean = false,
    var tracksConsumption: Boolean = false,
    var nonTrivialBacktracking: Boolean = false,
    var requiresCheckpointing: Boolean = false,
)

/**
 * Returns `true` if [this] is a capturing or non-capturing group,
 * that is trivial enough to match without a recursion when quantified.
 *
 * A quantified group qualified as trivial, could be match using [TrivialGroupQuantifierSet]
 * or [ReluctantTrivialGroupQuantifierSet], which match iteratively, without a recursion (there is no
 * corresponding possessive set, because possessively quantified sets are free of this problem).
 */
internal fun JointSet.isTrivialGroupForQuantification(): Boolean {
    val enclosedSet: AbstractSet
    val terminator: AbstractSet

    // Here, we're only considering capturing and non-capturing groups with a single child
    when (this) {
        is SingleSet -> {
            enclosedSet = kid
            terminator = fSet
        }
        is NonCapturingJointSet -> {
            enclosedSet = getSingleChildOrNull() ?: return false
            terminator = fSet
        }
        else -> return false
    }

    val properties = SetProperties().also { enclosedSet.collectProperties(it, terminator) }
    // Either of group capturing, checkpointing (for lookaround sets), or complicated backtracking make a set non-trivial.
    return !(properties.nonTrivialBacktracking || properties.capturesGroups || properties.requiresCheckpointing)
}
