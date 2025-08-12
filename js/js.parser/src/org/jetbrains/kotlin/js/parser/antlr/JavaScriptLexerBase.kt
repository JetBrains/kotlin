package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import java.util.*

/**
 * All lexer methods that used in grammar (IsStrictMode)
 * should start with Upper Case Char similar to Lexer rules.
 */
abstract class JavaScriptLexerBase(input: CharStream?) : Lexer(input) {
    /**
     * Stores values of nested modes. By default mode is strict or
     * defined externally (useStrictDefault)
     */
    private val scopeStrictModes: Deque<Boolean?> = ArrayDeque<Boolean?>()

    private var lastToken: Token? = null

    /**
     * Default value of strict mode
     * Can be defined externally by setUseStrictDefault
     */
    var strictDefault: Boolean = false
        private set

    /**
     * Current value of strict mode
     * Can be defined during parsing, see StringFunctions.js and StringGlobal.js samples
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
        return !templateDepthStack.isEmpty() && templateDepthStack.peek() == currentDepth
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

        if (next.getChannel() == Token.DEFAULT_CHANNEL) {
            // Keep track of the last token on the default channel.
            this.lastToken = next
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