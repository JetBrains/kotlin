/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.conversions.multiResolveFqName
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction

fun kotlinTypeByName(name: String, symbolProvider: JKSymbolProvider): JKClassType {
    val symbol =
        symbolProvider.provideDirectSymbol(
            resolveFqName(ClassId.fromString(name), symbolProvider.symbolsByPsi.keys.first())!!
        ) as JKClassSymbol
    return JKClassTypeImpl(symbol, emptyList())
}

private fun JKKtOperatorToken.methodSymbol(
    leftType: JKType,
    rightType: JKType,
    symbolProvider: JKSymbolProvider
): JKMethodSymbol {
    val classSymbol =
        when (leftType) {
            is JKClassType -> leftType.classReference as JKMultiverseKtClassSymbol
            is JKJavaPrimitiveType -> {
                val psiClass = resolveFqName(
                    ClassId.fromString(leftType.jvmPrimitiveType.primitiveType.typeFqName.asString()),
                    symbolProvider.symbolsByPsi.keys.first()
                )!!
                symbolProvider.provideDirectSymbol(psiClass) as JKMultiverseKtClassSymbol
            }
            else ->
                TODO(leftType::class.java.toString())
        }
    val operatorNames =
        if (operatorName == "equals") listOf("equals", "compareTo")
        else listOf(operatorName)
    return classSymbol.target.declarations// todo look for extensions
        .asSequence()
        .filterIsInstance<KtNamedFunction>()
        .filter { it.name in operatorNames }
        .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
        .firstOrNull { it.parameterTypes.singleOrNull()?.takeIf { it.isSubtypeOf(rightType, symbolProvider) } != null }!!
}


fun kotlinBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    token: JKKtOperatorToken,
    context: ConversionContext
): JKBinaryExpression? {
    val leftType = left.type(context)
    val rightType = right.type(context)
    val methodSymbol = token.methodSymbol(leftType, rightType, context.symbolProvider)
    return JKBinaryExpressionImpl(left, right, JKKtOperatorImpl(token, methodSymbol))
}

fun untilToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: ConversionContext,
    psiContext: PsiElement
): JKKtOperatorExpression =
    rangeExpression(
        from,
        to,
        "until",
        conversionContext,
        psiContext
    )

fun downToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: ConversionContext,
    psiContext: PsiElement
): JKKtOperatorExpression =
    rangeExpression(
        from,
        to,
        "downTo",
        conversionContext,
        psiContext
    )

fun rangeExpression(
    from: JKExpression,
    to: JKExpression,
    operatorName: String,
    conversionContext: ConversionContext,
    psiContext: PsiElement
): JKKtOperatorExpressionImpl {
    val symbol = conversionContext.symbolProvider.provideDirectSymbol(
        multiResolveFqName(ClassId.fromString("kotlin/ranges/$operatorName"), psiContext).first()
    ) as JKMethodSymbol
    return JKKtOperatorExpressionImpl(from, symbol, to)
}

fun blockStatement(vararg statements: JKStatement) =
    JKBlockStatementImpl(JKBlockImpl(statements.toList()))
