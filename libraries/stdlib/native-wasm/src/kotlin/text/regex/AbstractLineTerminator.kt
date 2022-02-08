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

@file:Suppress("DEPRECATION") // Char.toInt()
package kotlin.text.regex

private object unixLT : AbstractLineTerminator() {
    override fun isLineTerminator(codepoint: Int): Boolean = (codepoint == '\n'.toInt())
    override fun isLineTerminatorPair(char1: Char, char2: Char): Boolean = false
    override fun isAfterLineTerminator(previous: Char, checked: Char): Boolean = (previous == '\n')
}

private object unicodeLT : AbstractLineTerminator() {
    override fun isLineTerminatorPair(char1: Char, char2: Char): Boolean {
        return char1 == '\r' && char2 == '\n'
    }

    override fun isLineTerminator(codepoint: Int): Boolean {
        return codepoint == '\n'.toInt()
                || codepoint == '\r'.toInt()
                || codepoint == '\u0085'.toInt()
                || codepoint or 1 == '\u2029'.toInt()
    }

    override fun isAfterLineTerminator(previous: Char, checked: Char): Boolean {
        return previous == '\n' || previous == '\u0085' || previous.toInt() or 1 == '\u2029'.toInt()
                || previous == '\r' && checked != '\n'
    }
}

/**
 * Line terminator factory
 */
internal abstract class AbstractLineTerminator {

    /** Checks if the single character is a line terminator or not. */
    open fun isLineTerminator(char: Char): Boolean = isLineTerminator(char.toInt())

    /** Checks if the codepoint is a line terminator or not */
    abstract fun isLineTerminator(codepoint: Int): Boolean

    /** Checks if the pair of symbols is a line terminator (e.g. for \r\n case) */
    abstract fun isLineTerminatorPair(char1: Char, char2: Char): Boolean

    /** Checks if a [checked] character is after a line terminator using the [previous] character.*/
    abstract fun isAfterLineTerminator(previous: Char, checked: Char): Boolean

    companion object {
        fun getInstance(flag: Int): AbstractLineTerminator {
            if (flag and Pattern.UNIX_LINES != 0) {
                return unixLT
            } else {
                return unicodeLT
            }
        }
    }
}
