/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.IdentityStringSyntaxNode.LeafTargetSyntaxNode
import org.jetbrains.kotlin.commonizer.IdentityStringSyntaxNode.SharedTargetSyntaxNode
import org.jetbrains.kotlin.commonizer.IdentityStringToken.*

public fun parseCommonizerTargetOrNull(identityString: String): CommonizerTarget? {
    return try {
        parseCommonizerTarget(identityString)
    } catch (t: IllegalArgumentException) {
        null
    }
}

public fun parseCommonizerTarget(identityString: String): CommonizerTarget {
    try {
        val tokens = tokenizeIdentityString(identityString)
        val syntaxTree = parser(tokens) ?: error("Failed building syntax tree. $identityString")
        check(syntaxTree.remaining.isEmpty()) { "Failed building syntax tree. Unexpected remaining tokens ${syntaxTree.remaining}" }
        return buildCommonizerTarget(syntaxTree.value)
    } catch (e: Throwable) {
        throw IllegalArgumentException("Failed parsing CommonizerTarget from \"$identityString\"", e)
    }
}

//region Tokens

private fun tokenizeIdentityString(identityString: String): List<IdentityStringToken> {
    var remainingString = identityString
    val tokenizer = sharedTargetStartTokenizer + sharedTargetEndTokenizer + separatorTokenizer + wordTokenizer
    return mutableListOf<IdentityStringToken>().apply {
        while (remainingString.isNotEmpty()) {
            val generatedToken = tokenizer.nextToken(remainingString)
                ?: error("Unexpected token at $remainingString")

            remainingString = generatedToken.remaining
            add(generatedToken.token)
        }
    }.toList()
}

private sealed class IdentityStringToken {
    data class Word(val value: String) : IdentityStringToken()
    object Separator : IdentityStringToken()
    object SharedTargetStart : IdentityStringToken()
    object SharedTargetEnd : IdentityStringToken()

    final override fun toString(): String {
        return when (this) {
            is Word -> value
            is Separator -> ", "
            is SharedTargetStart -> "("
            is SharedTargetEnd -> ")"
        }
    }
}

private data class GeneratedToken(val token: IdentityStringToken, val remaining: String)

private interface IdentityStringTokenizer {
    fun nextToken(value: String): GeneratedToken?
}

private operator fun IdentityStringTokenizer.plus(other: IdentityStringTokenizer): IdentityStringTokenizer {
    return CompositeIdentityStringTokenizer(this, other)
}

private data class CompositeIdentityStringTokenizer(
    val first: IdentityStringTokenizer,
    val second: IdentityStringTokenizer
) : IdentityStringTokenizer {
    override fun nextToken(value: String): GeneratedToken? {
        return first.nextToken(value) ?: second.nextToken(value)
    }
}

private data class RegexIdentityStringTokenizer(
    val regex: Regex,
    val token: (String) -> IdentityStringToken
) : IdentityStringTokenizer {
    override fun nextToken(value: String): GeneratedToken? {
        val firstMatchResult = regex.findAll(value, 0).firstOrNull() ?: return null
        val range = firstMatchResult.range
        if (range.first != 0) return null
        return GeneratedToken(token(firstMatchResult.value), value.drop(firstMatchResult.value.length))
    }
}

private val sharedTargetStartTokenizer =
    RegexIdentityStringTokenizer(Regex.fromLiteral("(")) { SharedTargetStart }

private val sharedTargetEndTokenizer =
    RegexIdentityStringTokenizer(Regex.fromLiteral(")")) { SharedTargetEnd }

private val separatorTokenizer =
    RegexIdentityStringTokenizer(Regex("""\s*,\s*""")) { Separator }

private val wordTokenizer =
    RegexIdentityStringTokenizer(Regex("\\w+"), IdentityStringToken::Word)

//endregion

//region Syntax Tree

private val parser = anyOf(SharedTargetParser, LeafTargetParser)

private data class ParserOutput<out T : Any>(val value: T, val remaining: List<IdentityStringToken>)

private interface Parser<out T : Any> {
    operator fun invoke(tokens: List<IdentityStringToken>): ParserOutput<T>?
}


private fun <T : Any> anyOf(vararg parser: Parser<T>): Parser<T> {
    return AnyOfParser(parser.toList())
}

private data class AnyOfParser<T : Any>(val parsers: List<Parser<T>>) : Parser<T> {
    override fun invoke(tokens: List<IdentityStringToken>): ParserOutput<T>? {
        return parsers.mapNotNull { parser -> parser(tokens) }.firstOrNull()
    }
}

private fun <T : Any> Parser<T>.zeroOrMore(): Parser<List<T>> {
    return ZeroOrMoreParser(this)
}

private data class ZeroOrMoreParser<T : Any>(val parser: Parser<T>) : Parser<List<T>> {
    override fun invoke(tokens: List<IdentityStringToken>): ParserOutput<List<T>>? {
        val outputs = mutableListOf<T>()
        var remainingTokens = tokens
        while (true) {
            val output = parser(remainingTokens) ?: break
            if (output.remaining == remainingTokens) break
            outputs.add(output.value)
            remainingTokens = output.remaining
        }
        return ParserOutput(outputs.toList(), remainingTokens)
    }
}

private fun <T : Any> Parser<T>.ignore(token: IdentityStringToken): Parser<T> {
    return IgnoreTokensParser(this, token)
}

private data class IgnoreTokensParser<T : Any>(val parser: Parser<T>, val ignoredToken: IdentityStringToken) : Parser<T> {
    override fun invoke(tokens: List<IdentityStringToken>): ParserOutput<T>? {
        return parser(
            if (tokens.firstOrNull() == ignoredToken) tokens.drop(1) else tokens
        )
    }
}

private object LeafTargetParser : Parser<LeafTargetSyntaxNode> {
    override fun invoke(tokens: List<IdentityStringToken>): ParserOutput<LeafTargetSyntaxNode>? {
        val nextToken = tokens.firstOrNull() as? Word ?: return null
        return ParserOutput(LeafTargetSyntaxNode(nextToken), tokens.drop(1))
    }
}

private object SharedTargetParser : Parser<SharedTargetSyntaxNode> {
    override fun invoke(tokens: List<IdentityStringToken>): ParserOutput<SharedTargetSyntaxNode>? {
        if (tokens.firstOrNull() !is SharedTargetStart) return null

        val innerParser = anyOf(LeafTargetParser, SharedTargetParser).ignore(Separator).zeroOrMore()
        val innerParserOutput = innerParser(tokens.drop(1)) ?: return null

        val closingToken = innerParserOutput.remaining.firstOrNull()
        if (closingToken != SharedTargetEnd) {
            error("Missing '${SharedTargetEnd}' at ${tokens.joinToString("")}")
        }

        return ParserOutput(SharedTargetSyntaxNode(innerParserOutput.value), innerParserOutput.remaining.drop(1))
    }

}

private sealed class IdentityStringSyntaxNode {
    data class LeafTargetSyntaxNode(val token: Word) : IdentityStringSyntaxNode()
    data class SharedTargetSyntaxNode(val children: List<IdentityStringSyntaxNode>) : IdentityStringSyntaxNode()
}

//endregion Tree

//region Build CommonizerTarget

private fun buildCommonizerTarget(node: IdentityStringSyntaxNode): CommonizerTarget {
    return when (node) {
        is LeafTargetSyntaxNode -> LeafCommonizerTarget(node.token.value)
        is SharedTargetSyntaxNode -> SharedCommonizerTarget(
            node.children.map { child -> buildCommonizerTarget(child) }.toSet()
        )
    }
}

//endregion
