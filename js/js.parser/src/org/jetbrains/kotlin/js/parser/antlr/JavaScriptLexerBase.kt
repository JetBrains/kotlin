/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 by Bart Kiers (original author) and Alexandre Vitorelli (contributor -> ported to CSharp)
 * Copyright (c) 2017-2020 by Ivan Kochurkin (Positive Technologies):
    added ECMAScript 6 support, cleared and transformed to the universal grammar.
 * Copyright (c) 2018 by Juan Alvarez (contributor -> ported to Go)
 * Copyright (c) 2019 by Student Main (contributor -> ES2020)
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import java.util.*

abstract class JavaScriptLexerBase(input: CharStream?) : Lexer(input) {
    /**
     * Stores values of nested modes. By default mode is strict or
     * defined externally (useStrictDefault)
     */
    private val scopeStrictModes: Deque<Boolean?> = ArrayDeque<Boolean?>()

    private var lastToken: Token? = null

    private var offsetLine: Int = 0
    private var offsetColumn: Int = 0

    /**
     * Default value of strict mode
     * Can be defined externally by setUseStrictDefault
     */
    var strictDefault: Boolean = false
        private set

    /**
     * Current value of strict mode
     */
    private var useStrictCurrent = false

    /**
     * Preserves depth due to braces including template literals.
     */
    private var currentDepth = 0

    /**
     * Preserves the starting depth of template literals to correctly handle braces inside template literals.
     */
    private var templateDepthStack: Deque<Int?> = ArrayDeque<Int?>()

    fun isStartOfFile(): Boolean {
        return lastToken == null
    }

    fun setUseStrictDefault(value: Boolean) {
        this.strictDefault = value
        useStrictCurrent = value
    }

    fun isStrictMode(): Boolean {
        return useStrictCurrent
    }

    fun isInTemplateString(): Boolean {
        return templateDepthStack.isNotEmpty() && templateDepthStack.peek() == currentDepth
    }

    /**
     * Sets the shift that will be used to offset every token's position by the given amount while calling nextToken().
     */
    fun setTokenOffset(line: Int, column: Int) {
        offsetLine = line
        offsetColumn = column
    }

    /**
     * Return the next token from the character stream and records this last
     * token in case it resides on the default channel. This recorded token
     * is used to determine when the lexer could possibly match a regex
     * literal. Also changes scopeStrictModes stack if tokenize special
     * string 'use strict';
     *
     * @return the next token from the character stream.
     */
    override fun nextToken(): Token {
        val next = super.nextToken()

        if (next.channel == Token.DEFAULT_CHANNEL) {
            // Keep track of the last token on the default channel.
            this.lastToken = next
        }

        // This way we ensure we add proper offset to the token when embedding raw js() islands into the Kotlin source code.
        if (next is CommonToken && next.getType() != Token.EOF) {
            next.line = next.line + offsetLine
            next.charPositionInLine = next.charPositionInLine + offsetColumn
        }

        return next
    }

    protected fun processOpenBrace() {
        currentDepth++
        useStrictCurrent = if (scopeStrictModes.isNotEmpty() && scopeStrictModes.peek() == true) true else this.strictDefault
        scopeStrictModes.push(useStrictCurrent)
    }

    protected fun processCloseBrace() {
        useStrictCurrent = (if (scopeStrictModes.isNotEmpty()) scopeStrictModes.pop() else this.strictDefault)!!
        currentDepth--
    }

    protected fun processTemplateOpenBrace() {
        currentDepth++
        this.templateDepthStack.push(currentDepth)
    }

    protected fun processTemplateCloseBrace() {
        this.templateDepthStack.pop()
        currentDepth--
    }

    protected fun processStringLiteral() {
        if (lastToken == null || lastToken!!.getType() == JavaScriptLexer.OpenBrace) {
            val text = getText()
            if (text == "\"use strict\"" || text == "'use strict'") {
                if (scopeStrictModes.size > 0) scopeStrictModes.pop()
                useStrictCurrent = true
                scopeStrictModes.push(useStrictCurrent)
            }
        }
    }

    /**
     * Returns `true` if the lexer can match a regex literal.
     */
    protected fun isRegexPossible(): Boolean {
        if (this.lastToken == null) {
            // No token has been produced yet: at the start of the input,
            // no division is possible, so a regex literal _is_ possible.
            return true
        }

        return when (this.lastToken!!.type) {
            JavaScriptLexer.Identifier,
            JavaScriptLexer.NullLiteral,
            JavaScriptLexer.BooleanLiteral,
            JavaScriptLexer.This,
            JavaScriptLexer.CloseBracket,
            JavaScriptLexer.CloseParen,
            JavaScriptLexer.OctalIntegerLiteral,
            JavaScriptLexer.DecimalLiteral,
            JavaScriptLexer.HexIntegerLiteral,
            JavaScriptLexer.StringLiteral,
            JavaScriptLexer.PlusPlus,
            JavaScriptLexer.MinusMinus
                -> false // After any of the tokens above, no regex literal can follow.
            else -> true // In all other cases, a regex literal _is_ possible.
        }
    }

    override fun reset() {
        this.scopeStrictModes.clear()
        this.lastToken = null
        this.strictDefault = false
        this.useStrictCurrent = false
        this.currentDepth = 0
        this.templateDepthStack = ArrayDeque<Int?>()
        super.reset()
    }
}