/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.nullIfStubExpression
import org.jetbrains.kotlin.nj2k.qualified
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class MethodReferenceToLambdaConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethodReferenceExpression) return recurse(element)
        val symbol = element.identifier

        val parameters =
            if (symbol is JKMethodSymbol) {
                (symbol.parameterNames ?: return recurse(element)).zip(
                    symbol.parameterTypes ?: return recurse(element)
                ) { name, type ->
                    JKParameterImpl(
                        JKTypeElementImpl(type),
                        JKNameIdentifierImpl(name),
                        isVarArgs = false
                    )
                }
            } else emptyList()

        val receiverParameter = element.qualifier
            .nullIfStubExpression()
            ?.safeAs<JKClassAccessExpression>()
            ?.takeIf { symbol.safeAs<JKMethodSymbol>()?.isStatic == false && !element.isConstructorCall }
            ?.let { classAccessExpression ->
                JKParameterImpl(
                    JKTypeElementImpl(JKClassTypeImpl(classAccessExpression.identifier)),
                    JKNameIdentifierImpl(RECEIVER_NAME),
                    isVarArgs = false
                )
            }
        val arguments = parameters.map { parameter ->
            val parameterSymbol = context.symbolProvider.provideUniverseSymbol(parameter)
            JKArgumentImpl(JKFieldAccessExpressionImpl(parameterSymbol))
        }
        val callExpression =
            when (symbol) {
                is JKMethodSymbol ->
                    JKKtCallExpressionImpl(
                        symbol,
                        JKArgumentListImpl(arguments)
                    )
                is JKClassSymbol -> JKJavaNewExpressionImpl(symbol, JKArgumentListImpl(), JKTypeArgumentListImpl())
                is JKUnresolvedSymbol -> return recurse(element)
                else -> error("Symbol should be either method symbol or class symbol, but it is ${symbol::class}")
            }
        val qualifier = when {
            receiverParameter != null ->
                JKFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(receiverParameter))
            element.isConstructorCall -> element.qualifier.safeAs<JKQualifiedExpression>()?.let { it::receiver.detached() }
            else -> element::qualifier.detached().nullIfStubExpression()
        }


        val lambda = JKLambdaExpressionImpl(
            JKExpressionStatementImpl(callExpression.qualified(qualifier)),
            listOfNotNull(receiverParameter) + parameters,
            element::functionalType.detached(),
            JKTypeElementImpl(
                when (symbol) {
                    is JKMethodSymbol -> symbol.returnType ?: JKNoTypeImpl
                    is JKClassSymbol -> JKClassTypeImpl(symbol)
                    is JKUnresolvedSymbol -> return recurse(element)
                    else -> error("Symbol should be either method symbol or class symbol, but it is ${symbol::class}")
                }
            )
        )

        return recurse(lambda)
    }

    companion object {
        private const val RECEIVER_NAME = "obj" //name taken from old j2k
    }
}