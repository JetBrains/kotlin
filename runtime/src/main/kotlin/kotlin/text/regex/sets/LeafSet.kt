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
 * Base class for nodes representing leaf tokens of the RE, those who consumes fixed number of characters.
 */
internal abstract class LeafSet : SimpleSet(AbstractSet.TYPE_LEAF) {

    open val charCount = 1

    /** Returns "shift", the number of accepted chars. Commonly internal function, but called by quantifiers. */
    abstract fun accepts(startIndex: Int, testString: CharSequence): Int

    /**
     * Checks if we can enter this state and pass the control to the next one.
     * Return positive value if match succeeds, negative otherwise.
     */
    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        if (startIndex + charCount > testString.length) {
            return -1
        }

        val shift = accepts(startIndex, testString) // TODO: may be move the check above in accept function.
        if (shift < 0) {
            return -1
        }

        return next.matches(startIndex + shift, testString, matchResult)
    }

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean {
        return true
    }
}
