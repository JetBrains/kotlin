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

import kotlin.IllegalArgumentException

/**
 * Represents RE quantifier; contains two fields responsible for min and max number of repetitions.
 * -1 as a maximum number of repetition represents infinity(i.e. +,*).
 */
internal class Quantifier(val min: Int, val max: Int = min) : SpecialToken() {

    init {
        if (min < 0 || max < -1) {
            throw IllegalArgumentException("Incorrect quantifier value: $this")
        }
    }

    override fun toString() = "{$min, ${if (max == INF) "" else max}}"

    override val type: Type = SpecialToken.Type.QUANTIFIER

    companion object {
        val starQuantifier = Quantifier(0, -1)
        val plusQuantifier = Quantifier(1, -1)
        val altQuantifier  = Quantifier(0,  1)

        val INF = -1

        fun fromLexerToken(token: Int) = when(token) {
            Lexer.QUANT_STAR, Lexer.QUANT_STAR_P, Lexer.QUANT_STAR_R -> starQuantifier
            Lexer.QUANT_ALT, Lexer.QUANT_ALT_P, Lexer.QUANT_ALT_R -> altQuantifier
            Lexer.QUANT_PLUS, Lexer.QUANT_PLUS_P, Lexer.QUANT_PLUS_R -> plusQuantifier
            else -> throw IllegalArgumentException("Unknown quantifier token: $token")
        }
    }
}

