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

import kotlin.RuntimeException

/**
 * Generalized greedy quantifier node over the leaf nodes.
 *  - a{n,m};
 *  - a* == a{0, <inf>};
 *  - a? == a{0, 1};
 *  - a+ == a{1, <inf>};
 */
open internal class LeafQuantifierSet(var quantifier: Quantifier,
                                      innerSet: LeafSet,
                                      next: AbstractSet,
                                      type: Int
) : QuantifierSet(innerSet, next, type) {

    val leaf: LeafSet get() = super.innerSet as LeafSet
    val min: Int get() = quantifier.min
    val max: Int get() = quantifier.max

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        var occurrences = 0

        // Process first <min> occurrences of the sequence being looked for.
        while (occurrences < min) {
            if (index + leaf.charCount > testString.length) {
                return -1
            }

            val shift = leaf.accepts(index, testString)
            if (shift < 1) {
                return -1
            }
            index += shift
            occurrences++
        }

        // Process occurrences between min and max.
        while  ((max == Quantifier.INF || occurrences < max) && index + leaf.charCount <= testString.length) {
            val shift = leaf.accepts(index, testString)
            if (shift < 1) {
                break
            }
            index += shift
            occurrences++
        }

        // Roll back if the next node does't match the remaining string.
        while (occurrences >= min) {
            val shift = next.matches(index, testString, matchResult)
            if (shift >= 0) {
                return shift
            }
            index -= leaf.charCount
            occurrences--
        }
        return -1
    }

    override val name: String
        get() = quantifier.toString()

    override var innerSet: AbstractSet
        get() = super.innerSet
        set(innerSet) {
            if (innerSet !is LeafSet)
                throw RuntimeException("Internal Error")
            super.innerSet = innerSet
        }
}
