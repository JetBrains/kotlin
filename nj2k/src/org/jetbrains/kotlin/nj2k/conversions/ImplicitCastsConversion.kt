/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.isEquals
import org.jetbrains.kotlin.nj2k.parenthesizeIfBinaryExpression
import org.jetbrains.kotlin.nj2k.symbols.JKMethodSymbol
import org.jetbrains.kotlin.nj2k.symbols.isUnresolved
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.*

import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ImplicitCastsConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKVariable -> convertVariable(element)
            is JKCallExpression -> convertMethodCallExpression(element)
            is JKBinaryExpression -> return recurse(convertBinaryExpression(element))
            is JKKtAssignmentStatement -> convertAssignmentStatement(element)
        }
        return recurse(element)
    }


    private fun convertBinaryExpression(binaryExpression: JKBinaryExpression): JKExpression {
        fun JKBinaryExpression.convertBinaryOperationWithChar(): JKBinaryExpression {
            val leftType = left.calculateType(typeFactory)?.asPrimitiveType() ?: return this
            val rightType = right.calculateType(typeFactory)?.asPrimitiveType() ?: return this

            val leftOperandCastedCasted by lazy(LazyThreadSafetyMode.NONE) {
                JKBinaryExpression(
                    ::left.detached().let { it.castTo(rightType, strict = true) ?: it },
                    ::right.detached(),
                    operator
                )
            }

            val rightOperandCastedCasted by lazy(LazyThreadSafetyMode.NONE) {
                JKBinaryExpression(
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

        return binaryExpression.convertBinaryOperationWithChar()
    }

    private fun convertVariable(variable: JKVariable) {
        if (variable.initializer is JKStubExpression) return
        variable.initializer.castTo(variable.type.type)?.also {
            variable.initializer = it
        }
    }

    private fun convertAssignmentStatement(statement: JKKtAssignmentStatement) {
        val expressionType = statement.field.calculateType(typeFactory) ?: return
        statement.expression.castTo(expressionType)?.also {
            statement.expression = it
        }
    }


    private fun convertMethodCallExpression(expression: JKCallExpression) {
        if (expression.identifier.isUnresolved) return
        val parameterTypes = expression.identifier.parameterTypesWithLastArgumentUnfoldedAsVararg() ?: return
        val newArguments = expression.arguments.arguments.mapIndexed { argumentIndex, argument ->
            val toType = parameterTypes.getOrNull(argumentIndex) ?: parameterTypes.last()
            argument.value.castTo(toType)
        }
        val needUpdate = newArguments.any { it != null }
        if (needUpdate) {
            for ((newArgument, oldArgument) in newArguments zip expression.arguments.arguments) {
                if (newArgument != null) {
                    oldArgument.value = newArgument.copyTreeAndDetach()
                }
            }
        }
    }

    private fun JKExpression.castStringToRegex(toType: JKType): JKExpression? {
        if (toType.safeAs<JKClassType>()?.classReference?.fqName != "java.util.regex.Pattern") return null
        val expressionType = calculateType(typeFactory) ?: return null
        if (!expressionType.isStringType()) return null
        return JKQualifiedExpression(
            copyTreeAndDetach().parenthesizeIfBinaryExpression(),
            JKCallExpressionImpl(
                symbolProvider.provideMethodSymbol("kotlin.text.toRegex"),
                JKArgumentList(),
                JKTypeArgumentList()
            )
        )

    }

    private fun JKExpression.castToAsPrimitiveTypes(toType: JKType, strict: Boolean): JKExpression? {
        if (this is JKPrefixExpression
            && (operator.token == JKOperatorToken.PLUS || operator.token == JKOperatorToken.MINUS)
        ) {
            val casted = expression.castToAsPrimitiveTypes(toType, strict) ?: return null
            return JKPrefixExpression(casted, operator)
        }
        val expressionTypeAsPrimitive = calculateType(typeFactory)?.asPrimitiveType() ?: return null
        val toTypeAsPrimitive = toType.asPrimitiveType() ?: return null
        if (toTypeAsPrimitive == expressionTypeAsPrimitive) return null

        if (this is JKLiteralExpression) {
            if (!strict
                && expressionTypeAsPrimitive == JKJavaPrimitiveType.INT
                && (toTypeAsPrimitive == JKJavaPrimitiveType.LONG ||
                        toTypeAsPrimitive == JKJavaPrimitiveType.SHORT ||
                        toTypeAsPrimitive == JKJavaPrimitiveType.BYTE)
            ) return null
            val expectedType = toTypeAsPrimitive.toLiteralType() ?: JKLiteralExpression.LiteralType.INT

            if (expressionTypeAsPrimitive.isNumberType() && toTypeAsPrimitive.isNumberType()) {
                return JKLiteralExpression(
                    literal,
                    expectedType
                )
            }
        }

        val initialTypeName = expressionTypeAsPrimitive.jvmPrimitiveType.javaKeywordName.capitalize()
        val conversionFunctionName = "to${toTypeAsPrimitive.jvmPrimitiveType.javaKeywordName.capitalize()}"
        return JKQualifiedExpression(
            copyTreeAndDetach().parenthesizeIfBinaryExpression(),
            JKCallExpressionImpl(
                symbolProvider.provideMethodSymbol("kotlin.$initialTypeName.$conversionFunctionName"),
                JKArgumentList()
            )
        )
    }


    private fun JKExpression.castTo(toType: JKType, strict: Boolean = false): JKExpression? {
        val expressionType = calculateType(typeFactory)
        if (expressionType == toType) return null
        castToAsPrimitiveTypes(toType, strict)?.also { return it }
        castStringToRegex(toType)?.also { return it }
        return null
    }

    private fun JKMethodSymbol.parameterTypesWithLastArgumentUnfoldedAsVararg(): List<JKType>? {
        val realParameterTypes = parameterTypes ?: return null
        if (realParameterTypes.isEmpty()) return null
        val lastArrayType = realParameterTypes.lastOrNull()?.arrayInnerType() ?: return realParameterTypes
        return realParameterTypes.subList(0, realParameterTypes.lastIndex) + lastArrayType
    }
}