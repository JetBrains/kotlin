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
 * Possessive quantifier node over a leaf node.
 *  - a{n,m}+;
 *  - a*+ == a{0, <inf>}+;
 *  - a?+ == a{0, 1}+;
 *  - a++ == a{1, <inf>}+;
 */
internal class PossessiveLeafQuantifierSet(
        quant: Quantifier,
        innerSet: LeafSet,
        next: AbstractSet, type: Int
) : LeafQuantifierSet(quant, innerSet, next, type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        var occurrences = 0

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

        while ((max == Quantifier.INF || occurrences < max) && index + leaf.charCount <= testString.length) {
            val shift = leaf.accepts(index, testString)
            if (shift < 1) {
                break
            }
            index += shift
            occurrences++
        }

        return next.matches(index, testString, matchResult)
    }
}
