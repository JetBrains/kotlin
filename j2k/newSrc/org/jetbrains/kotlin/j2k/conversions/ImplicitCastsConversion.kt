/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.bangedBangedExpr
import org.jetbrains.kotlin.j2k.copyTreeAndDetach
import org.jetbrains.kotlin.j2k.fixLiteral
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class ImplicitCastsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKVariable -> convertVariable(element)
            is JKForInStatement -> convertForInStatement(element)
            is JKMethodCallExpression -> convertMethodCallExpression(element)
            is JKKtAssignmentStatement -> convertAssignmentStatement(element)
        }
        return recurse(element)
    }

    private fun convertVariable(variable: JKVariable) {
        if (variable.initializer is JKStubExpression) return
        variable.initializer.castTo(variable.type.type)?.also {
            variable.initializer = it
        }
    }

    private fun convertForInStatement(forInStatement: JKForInStatement) {
        val notNullType = forInStatement.iterationExpression.type(context)?.updateNullability(Nullability.NotNull) ?: return
        forInStatement.iterationExpression.addBangBang(notNullType)?.also {
            forInStatement.iterationExpression = it
        }
    }

    private fun convertAssignmentStatement(statement: JKKtAssignmentStatement) {
        val expressionType = statement.field.type(context) ?: return
        statement.expression.castTo(expressionType)?.also {
            statement.expression = it
        }
    }


    private fun convertMethodCallExpression(expression: JKMethodCallExpression) {
        if (expression.identifier.isUnresolved()) return
        val parameterTypes = expression.identifier.parameterTypesWithUnfoldedVarargs() ?: return
        val newArguments =
            (expression.arguments.expressions.asSequence() zip parameterTypes)
                .map { (expression, toType) ->
                    expression.castTo(toType)
                }.toList()
        val needUpdate = newArguments.any { it != null }
        if (needUpdate) {
            expression.arguments = JKExpressionListImpl(
                (newArguments zip expression.arguments.expressions)
                    .map { (newArgument, oldArgument) ->
                        (newArgument ?: oldArgument).copyTreeAndDetach()
                    }
            )
        }
    }


    private fun JKExpression.addBangBang(toType: JKType): JKExpression? {
        val expressionType = type(context) as? JKClassType ?: return null
        if (toType !is JKClassType) return null
        if (expressionType.classReference == toType.classReference
            && expressionType.isNullable() && !toType.isNullable()
        ) {
            return this.copyTreeAndDetach().bangedBangedExpr(context)
        }
        return null
    }

    private fun JKExpression.castToAsPrimitiveTypes(toType: JKType): JKExpression? {
        val expressionTypeAsPrimitive = type(context)?.asPrimitiveType() ?: return null
        val toTypeAsPrimitive = toType.asPrimitiveType() ?: return null
        if (toTypeAsPrimitive == expressionTypeAsPrimitive) return null

        if (this is JKLiteralExpression) {
            if (expressionTypeAsPrimitive == JKJavaPrimitiveTypeImpl.INT
                && (toTypeAsPrimitive == JKJavaPrimitiveTypeImpl.LONG ||
                        toTypeAsPrimitive == JKJavaPrimitiveTypeImpl.SHORT ||
                        toTypeAsPrimitive == JKJavaPrimitiveTypeImpl.BYTE)
            ) return null
            val expectedType = toTypeAsPrimitive.toLiteralType() ?: JKLiteralExpression.LiteralType.INT

            if (expressionTypeAsPrimitive.isNumberType() && toTypeAsPrimitive.isNumberType()) {
                return JKJavaLiteralExpressionImpl(
                    literal,
                    expectedType
                ).fixLiteral(expectedType)
            }
        }

        val initialTypeName = expressionTypeAsPrimitive.jvmPrimitiveType.javaKeywordName.capitalize()
        val conversionFunctionName = "to${toTypeAsPrimitive.jvmPrimitiveType.javaKeywordName.capitalize()}"
        return JKQualifiedExpressionImpl(
            this.copyTreeAndDetach(),
            JKKtQualifierImpl.DOT,
            JKJavaMethodCallExpressionImpl(
                context.symbolProvider.provideByFqName("kotlin.$initialTypeName.$conversionFunctionName"),
                JKExpressionListImpl()
            )
        )
    }


    private fun JKExpression.castTo(toType: JKType): JKExpression? {
        val expressionType = type(context)
        if (expressionType == toType) return null
        castToAsPrimitiveTypes(toType)?.also { return it }
        return addBangBang(toType)
    }
}