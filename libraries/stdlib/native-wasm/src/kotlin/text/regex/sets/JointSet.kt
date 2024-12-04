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
 * Represents group, which is alternation of other subexpression.
 * One should think about "group" in this model as JointSet opening group and corresponding FSet closing group.
 */
open internal class JointSet(children: List<AbstractSet>, fSet: FSet) : AbstractSet() {

    protected var children: MutableList<AbstractSet> = mutableListOf<AbstractSet>().apply { addAll(children) }

    var fSet: FSet = fSet
        protected set

    var groupIndex: Int = fSet.groupIndex
        protected set

    /**
     * Returns startIndex+shift, the next position to match
     */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        if (children.isEmpty()) {
            return -1
        }
        val oldStart = matchResult.getStart(groupIndex)
        matchResult.setStart(groupIndex, startIndex)
        children.forEach {
            val shift = it.matches(startIndex, testString, matchResult)
            if (shift >= 0) {
                return shift
            }
        }
        matchResult.setStart(groupIndex, oldStart)
        return -1
    }

    override var next: AbstractSet
        get() = fSet.next
        set(next) {
            fSet.next = next
        }

    override val name: String
            get() = "JointSet"
    override fun first(set: AbstractSet): Boolean = children.any { it.first(set) }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        return !(matchResult.getEnd(groupIndex) >= 0 && matchResult.getStart(groupIndex) == matchResult.getEnd(groupIndex))
    }

    override fun processSecondPassInternal(): AbstractSet {
        val fSet = this.fSet
        if (!fSet.secondPassVisited) {
            val newFSet = fSet.processSecondPass()
            @OptIn(ExperimentalNativeApi::class)
            assert(newFSet == fSet)
        }

        @OptIn(ExperimentalNativeApi::class)
        children.replaceAll { child -> if (!child.secondPassVisited) child.processSecondPass() else child }
        return super.processSecondPassInternal()
    }
}
