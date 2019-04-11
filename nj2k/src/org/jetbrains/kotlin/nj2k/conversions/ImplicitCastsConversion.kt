/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ImplicitCastsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKVariable -> convertVariable(element)
            is JKForInStatement -> convertForInStatement(element)
            is JKMethodCallExpression -> convertMethodCallExpression(element)
            is JKBinaryExpression -> return recurse(convertBinaryExpression(element))
            is JKKtAssignmentStatement -> convertAssignmentStatement(element)
        }
        return recurse(element)
    }


    private fun convertBinaryExpression(binaryExpression: JKBinaryExpression): JKExpression {
        fun JKBinaryExpression.addBangBang(): JKBinaryExpression {
            fun JKType.isAcceptable() = asPrimitiveType() != null

            if (operator.isEquals()) return this

            val leftType = left.type(context.symbolProvider) ?: return this
            val rightType = right.type(context.symbolProvider) ?: return this
            return if (leftType.isAcceptable()
                && rightType.isAcceptable()
                && (leftType.isNullable() || rightType.isNullable())
                && (operator.isArithmetic() || operator.isLessOrGreater())
            ) {
                JKBinaryExpressionImpl(
                    ::left.detached().bangedBangedExpr(context.symbolProvider),
                    ::right.detached().bangedBangedExpr(context.symbolProvider),
                    operator
                )
            } else this
        }

        fun JKBinaryExpression.convertBinaryOperationWithChar(): JKBinaryExpression {
            val leftType = left.type(context.symbolProvider)?.asPrimitiveType() ?: return this
            val rightType = right.type(context.symbolProvider)?.asPrimitiveType() ?: return this

            val leftOperandCastedCasted by lazy {
                JKBinaryExpressionImpl(
                    ::left.detached().let { it.castTo(rightType, strict = true) ?: it },
                    ::right.detached(),
                    operator
                )
            }

            val rightOperandCastedCasted by lazy {
                JKBinaryExpressionImpl(
                    ::left.detached(),
                    ::right.detached().let { it.castTo(leftType, strict = true) ?: it },
                    operator

                )
            }

            return when {
                leftType.jvmPrimitiveType == rightType.jvmPrimitiveType -> this
                leftType.jvmPrimitiveType == JvmPrimitiveType.CHAR -> leftOperandCastedCasted
                rightType.jvmPrimitiveType == JvmPrimitiveType.CHAR -> rightOperandCastedCasted
                operator.isEquals() ->
                    if (rightType isStrongerThan leftType) leftOperandCastedCasted
                    else rightOperandCastedCasted
                else -> this
            }
        }

        return binaryExpression.convertBinaryOperationWithChar().addBangBang()
    }

    private fun convertVariable(variable: JKVariable) {
        if (variable.initializer is JKStubExpression) return
        variable.initializer.castTo(variable.type.type)?.also {
            variable.initializer = it
        }
    }

    private fun convertForInStatement(forInStatement: JKForInStatement) {
        val notNullType = forInStatement.iterationExpression.type(context.symbolProvider)?.updateNullability(Nullability.NotNull) ?: return
        forInStatement.iterationExpression.addBangBang(notNullType)?.also {
            forInStatement.iterationExpression = it
        }
    }

    private fun convertAssignmentStatement(statement: JKKtAssignmentStatement) {
        val expressionType = statement.field.type(context.symbolProvider) ?: return
        statement.expression.castTo(expressionType)?.also {
            statement.expression = it
        }
    }


    private fun convertMethodCallExpression(expression: JKMethodCallExpression) {
        if (expression.identifier.isUnresolved()) return
        val parameterTypes = expression.identifier.parameterTypesWithUnfoldedVarargs() ?: return
        val newArguments =
            (expression.arguments.arguments.asSequence() zip parameterTypes)
                .map { (expression, toType) ->
                    expression.value.castTo(toType)
                }.toList()
        val needUpdate = newArguments.any { it != null }
        if (needUpdate) {
            for ((newArgument, oldArgument) in newArguments zip expression.arguments.arguments) {
                if (newArgument != null) {
                    oldArgument.value = newArgument.copyTreeAndDetach()
                }
            }
        }
    }


    private fun JKExpression.addBangBang(toType: JKType): JKExpression? {
        if (this is JKJavaNewExpression) return null
        val expressionType = type(context.symbolProvider) as? JKClassType ?: return null
        if (toType !is JKClassType) return null
        if (expressionType.classReference == toType.classReference
            && expressionType.isNullable() && !toType.isNullable()
        ) {
            return this.copyTreeAndDetach().bangedBangedExpr(context.symbolProvider)
        }
        return null
    }

    private fun JKExpression.castStringToRegex(toType: JKType): JKExpression? {
        if (toType.safeAs<JKClassType>()?.classReference?.fqName != "java.util.regex.Pattern") return null
        val expressionType = type(context.symbolProvider) ?: return null
        if (!expressionType.isStringType()) return null
        return JKQualifiedExpressionImpl(
            copyTreeAndDetach().parenthesizeIfBinaryExpression(),
            JKKtQualifierImpl.DOT,
            JKKtCallExpressionImpl(
                context.symbolProvider.provideByFqName("kotlin.text.toRegex", multiResolve = true),
                JKArgumentListImpl(),
                JKTypeArgumentListImpl()
            )
        )

    }

    private fun JKExpression.castToAsPrimitiveTypes(toType: JKType, strict: Boolean): JKExpression? {
        if (this is JKPrefixExpression
            && (operator.token.text == "+" || operator.token.text == "-")
        ) {
            val casted = expression.castToAsPrimitiveTypes(toType, strict) ?: return null
            return JKPrefixExpressionImpl(casted, operator)
        }
        val expressionTypeAsPrimitive = type(context.symbolProvider)?.asPrimitiveType() ?: return null
        val toTypeAsPrimitive = toType.asPrimitiveType() ?: return null
        if (toTypeAsPrimitive == expressionTypeAsPrimitive) return null

        if (this is JKLiteralExpression) {
            if (!strict
                && expressionTypeAsPrimitive == JKJavaPrimitiveTypeImpl.INT
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
                JKArgumentListImpl()
            )
        )
    }


    private fun JKExpression.castTo(toType: JKType, strict: Boolean = false): JKExpression? {
        val expressionType = type(context.symbolProvider)
        if (expressionType == toType) return null
        castToAsPrimitiveTypes(toType, strict)?.also { return it }
        castStringToRegex(toType)?.also { return it }
        return addBangBang(toType)
    }
}