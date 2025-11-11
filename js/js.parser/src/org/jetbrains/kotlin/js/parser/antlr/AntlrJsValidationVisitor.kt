/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.jetbrains.kotlin.js.parser.ErrorReporter
import org.antlr.v4.runtime.ParserRuleContext
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

internal class AntlrJsValidationVisitor(private val reporter: ErrorReporter) : AntlrJsBaseVisitor<ParserRuleContext?>() {
    override fun visitVariableDeclarationList(ctx: JavaScriptParser.VariableDeclarationListContext): ParserRuleContext? {
        if (ctx.varModifier().Const() != null)
            ctx.variableDeclaration()?.forEach {
                if (it.singleExpression() == null) {
                    reporter.error(
                        "Immutable variable declaration should have an initializer.",
                        it.assignable().startPosition,
                        it.assignable().stopPosition
                    )
                }
            }

        return super.visitVariableDeclarationList(ctx)
    }

    override fun visitAssignmentExpression(ctx: JavaScriptParser.AssignmentExpressionContext): ParserRuleContext? {
        if (ctx.lhs is JavaScriptParser.ThisExpressionContext)
            reporter.error("Invalid assignment left-hand side.", ctx.lhs.startPosition, ctx.lhs.stopPosition)

        return super.visitAssignmentExpression(ctx)
    }

    override fun visitAssignmentOperatorExpression(ctx: JavaScriptParser.AssignmentOperatorExpressionContext): ParserRuleContext? {
        if (ctx.lhs is JavaScriptParser.ThisExpressionContext)
            reporter.error("Invalid assignment left-hand side.", ctx.lhs.startPosition, ctx.lhs.stopPosition)

        return super.visitAssignmentOperatorExpression(ctx)
    }

    override fun visitDeleteExpression(ctx: JavaScriptParser.DeleteExpressionContext?): ParserRuleContext? {
        val expr = ctx?.singleExpressionImpl()
        if (expr != null && expr !is JavaScriptParser.MemberIndexExpressionContext && expr !is JavaScriptParser.MemberDotExpressionContext)
            reporter.error(
                "Wrong argument for ''delete'' operation. Must be either property reference or array subscript expression.",
                expr.startPosition, expr.stopPosition
            )

        return super.visitDeleteExpression(ctx)
    }
}

