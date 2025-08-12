package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.TokenStream
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

/**
 * All parser methods that used in grammar (p, prev, notLineTerminator, etc.)
 * should start with lower case char similar to parser rules.
 */
abstract class JavaScriptParserBase(input: TokenStream?) : Parser(input) {
    /**
     * Short form for prev(String str)
     */
    protected fun p(str: String?): Boolean {
        return prev(str)
    }

    /**
     * Whether the previous token value equals to @param str
     */
    protected fun prev(str: String?): Boolean {
        return _input.LT(-1).text == str
    }

    /**
     * Short form for next(String str)
     */
    protected fun n(str: String?): Boolean {
        return next(str)
    }

    /**
     * Whether the next token value equals to @param str
     */
    protected fun next(str: String?): Boolean {
        return _input.LT(1).getText() == str
    }

    protected fun notLineTerminator(): Boolean {
        return !lineTerminatorAhead()
    }

    protected fun notOpenBraceAndNotFunction(): Boolean {
        val nextTokenType = _input.LT(1).type
        return nextTokenType != JavaScriptParser.OpenBrace && nextTokenType != JavaScriptParser.Function_
    }

    protected fun closeBrace(): Boolean {
        return _input.LT(1).type == JavaScriptParser.CloseBrace
    }

    /**
     * Returns `true` iff on the current index of the parser's
     * token stream a token exists on the `HIDDEN` channel which
     * either is a line terminator, or is a multi line comment that
     * contains a line terminator.
     *
     * @return `true` iff on the current index of the parser's
     * token stream a token exists on the `HIDDEN` channel which
     * either is a line terminator, or is a multi line comment that
     * contains a line terminator.
     */
    protected fun lineTerminatorAhead(): Boolean {
        // Get the token ahead of the current index.

        var possibleIndexEosToken = this.currentToken.tokenIndex - 1
        if (possibleIndexEosToken < 0) return false
        var ahead = _input.get(possibleIndexEosToken)

        if (ahead.channel != Lexer.HIDDEN || ahead.channel != JavaScriptLexer.COMMENTS) {
            // We're only interested in tokens on the HIDDEN channel.
            return false
        }

        if (ahead.type == JavaScriptParser.LineTerminator) {
            // There is definitely a line terminator ahead.
            return true
        }

        if (ahead.type == JavaScriptParser.WhiteSpaces) {
            // Get the token ahead of the current whitespaces.
            possibleIndexEosToken = this.currentToken.tokenIndex - 2
            if (possibleIndexEosToken < 0) return false
            ahead = _input.get(possibleIndexEosToken)
        }

        // Get the token's text and type.
        val text = ahead.text
        val type = ahead.type

        // Check if the token is, or contains a line terminator.
        return type == JavaScriptParser.MultiLineComment && (text.contains("\r") || text.contains("\n"))
                || type == JavaScriptParser.LineTerminator
    }
}