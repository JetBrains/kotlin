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
 * Represents node accepting single character from the given char class.
 */
open internal class RangeSet(charClass: AbstractCharClass, val ignoreCase: Boolean = false) : LeafSet() {

    val chars: AbstractCharClass = charClass.instance

    override fun accepts(startIndex: Int, testString: CharSequence): Int {
        if (ignoreCase) {
            val char = testString[startIndex]
            return if (chars.contains(char.toUpperCase()) || chars.contains(char.toLowerCase())) 1 else -1
        } else {
            return if (chars.contains(testString[startIndex])) 1 else -1
        }
    }

    override val name: String
        get() = "range:" + (if (chars.alt) "^ " else " ") + chars.toString()

    override fun first(set: AbstractSet): Boolean {
        return when (set) {
            is CharSet -> AbstractCharClass.intersects(chars, set.char.toInt())
            is RangeSet -> AbstractCharClass.intersects(chars, set.chars)
            is SupplementaryCharSet -> false
            is SupplementaryRangeSet -> AbstractCharClass.intersects(chars, set.chars)
            else -> true
        }
    }
}
