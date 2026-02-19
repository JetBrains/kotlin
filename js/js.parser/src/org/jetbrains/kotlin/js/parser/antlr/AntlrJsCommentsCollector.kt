/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptLexer
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParserBaseVisitor

internal class AntlrJsCommentsCollector(private val tokenStream: CommonTokenStream) : JavaScriptParserBaseVisitor<Unit>() {
    private val consumedCommentIndexes = mutableSetOf<Int>()

    override fun visitStatement(ctx: JavaScriptParser.StatementContext) {
        ctx.collectCommentsBeforeNode()
        visitChildren(ctx)
        ctx.collectCommentsAfterNode()
    }

    override fun visitAssignmentExpression(ctx: JavaScriptParser.AssignmentExpressionContext) {
        ctx.collectCommentsBeforeNode()
        visitChildren(ctx)
        ctx.collectCommentsAfterNode()
    }

    override fun visitSingleExpression(ctx: JavaScriptParser.SingleExpressionContext) {
        ctx.collectCommentsBeforeNode()
        visitChildren(ctx)
        ctx.collectCommentsAfterNode()
    }

    override fun visitFormalParameterArg(ctx: JavaScriptParser.FormalParameterArgContext) {
        ctx.collectCommentsBeforeNode()
        visitChildren(ctx)
        ctx.collectCommentsAfterNode()
    }

    override fun visitArrowFunctionParameters(ctx: JavaScriptParser.ArrowFunctionParametersContext) {
        // Single-parameter without a parens (e.g. `abc => 123;`) should be handled here, separately.
        // Multiple parameters case is handled in `visitFormalParameterArg`.
        ctx.identifierName()?.let { singleIdentifier ->
            singleIdentifier.collectCommentsBeforeNode()
            visitChildren(singleIdentifier)
            singleIdentifier.collectCommentsAfterNode()
        }

        visitChildren(ctx)
    }

    private fun JavaScriptRuleContext.collectCommentsBeforeNode() {
        val beforeCommentTokens = tokenStream
            .getHiddenTokensToLeft(start.tokenIndex, JavaScriptLexer.COMMENTS)
            ?.filter { it.tokenIndex !in consumedCommentIndexes } ?: emptyList()
        commentsBefore.clear()
        beforeCommentTokens.forEach { token ->
            commentsBefore += token
            consumedCommentIndexes += token.tokenIndex
        }
    }

    private fun JavaScriptRuleContext.collectCommentsAfterNode() {
        val afterCommentTokens = tokenStream
            .getHiddenTokensToRight(stop.tokenIndex, JavaScriptLexer.COMMENTS)
            ?.filter { it.tokenIndex !in consumedCommentIndexes } ?: emptyList()
        commentsAfter.clear()
        afterCommentTokens.forEach { token ->
            commentsAfter += token
            consumedCommentIndexes += token.tokenIndex
        }
    }
}