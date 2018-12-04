/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.conversions.multiResolveFqName
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction

fun kotlinTypeByName(name: String, symbolProvider: JKSymbolProvider, nullability: Nullability = Nullability.Nullable): JKClassType {
    val symbol =
        symbolProvider.provideDirectSymbol(
            resolveFqName(ClassId.fromString(name), symbolProvider.symbolsByPsi.keys.first())!!
        ) as JKClassSymbol
    return JKClassTypeImpl(symbol, emptyList(), nullability)
}

private fun JKType.classSymbol(symbolProvider: JKSymbolProvider) =
    when (this) {
        is JKClassType -> classReference
        is JKJavaPrimitiveType -> {
            val psiClass = resolveFqName(
                ClassId.fromString(jvmPrimitiveType.primitiveType.typeFqName.asString()),
                symbolProvider.symbolsByPsi.keys.first()
            )!!
            symbolProvider.provideDirectSymbol(psiClass) as JKClassSymbol
        }
        else -> TODO(this::class.java.toString())
    }

private fun JKKtOperatorToken.binaryExpressionMethodSymbol(
    leftType: JKType,
    rightType: JKType,
    symbolProvider: JKSymbolProvider
): JKMethodSymbol {
    val operatorNames =
        if (operatorName == "equals") listOf("equals", "compareTo")
        else listOf(operatorName)

    fun PsiClass.methodSymbol() =
        allMethods
            .filter { it.name in operatorNames }
            .firstOrNull {
                it.parameterList.parameters.singleOrNull()?.takeIf {
                    rightType.isSubtypeOf(
                        it.type.toJK(symbolProvider),
                        symbolProvider
                    )
                } != null
            }?.let { symbolProvider.provideDirectSymbol(it) as JKMethodSymbol }


    val classSymbol = leftType.classSymbol(symbolProvider)
    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->
            classSymbol.target.declarations
                .asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name in operatorNames }
                .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
                .firstOrNull { it.parameterTypes.singleOrNull()?.takeIf { rightType.isSubtypeOf(it, symbolProvider) } != null }!!
        is JKUniverseClassSymbol -> classSymbol.target.psi<PsiClass>()?.methodSymbol()!!
        is JKMultiverseClassSymbol -> classSymbol.target.methodSymbol()!!

        else -> TODO(classSymbol::class.toString())
    }
}

private fun JKKtOperatorToken.unaryExpressionMethodSymbol(
    operandType: JKType,
    symbolProvider: JKSymbolProvider
): JKMethodSymbol {
    val classSymbol = operandType.classSymbol(symbolProvider)
    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->// todo look for extensions
            classSymbol.target.declarations.asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name == operatorName }
                .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
                .firstOrNull()!!
        else -> TODO(classSymbol::class.toString())
    }
}


fun kotlinBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    token: JKKtOperatorToken,
    context: ConversionContext
): JKBinaryExpression? {
    val leftType = left.type(context) ?: return null
    val rightType = right.type(context) ?: return null
    val methodSymbol = token.binaryExpressionMethodSymbol(leftType, rightType, context.symbolProvider)
    return JKBinaryExpressionImpl(left, right, JKKtOperatorImpl(token, methodSymbol))
}

fun kotlinPrefixExpression(
    operand: JKExpression,
    token: JKKtOperatorToken,
    context: ConversionContext
): JKPrefixExpression? {
    val operandType = operand.type(context) ?: return null
    val methodSymbol = token.unaryExpressionMethodSymbol(operandType, context.symbolProvider)
    return JKPrefixExpressionImpl(operand, JKKtOperatorImpl(token, methodSymbol))
}

fun kotlinPostfixExpression(
    operand: JKExpression,
    token: JKKtOperatorToken,
    context: ConversionContext
): JKPostfixExpression? {
    val operandType = operand.type(context) ?: return null
    val methodSymbol = token.unaryExpressionMethodSymbol(operandType, context.symbolProvider)
    return JKPostfixExpressionImpl(operand, JKKtOperatorImpl(token, methodSymbol))
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

fun useExpression(
    receiver: JKExpression,
    variableIdentifier: JKNameIdentifier,
    body: JKStatement,
    symbolProvider: JKSymbolProvider
): JKExpression {
    val useSymbol =
        symbolProvider
            .provideDirectSymbol(
                resolveFqName(
                    ClassId.fromString("kotlin.io.use"),
                    symbolProvider.symbolsByPsi.keys.first()
                )!!
            ) as JKMethodSymbol
    val lambdaParameter =
        JKParameterImpl(JKTypeElementImpl(JKNoTypeImpl), variableIdentifier)

    val lambda = JKLambdaExpressionImpl(
        listOf(lambdaParameter),
        body
    )
    val methodCall =
        JKJavaMethodCallExpressionImpl(
            useSymbol,
            JKExpressionListImpl(listOf(lambda))
        )
    return JKQualifiedExpressionImpl(receiver, JKKtQualifierImpl.DOT, methodCall)
}

fun kotlinAssert(assertion: JKExpression, message: JKExpression?, symbolProvider: JKSymbolProvider) =
    JKKtCallExpressionImpl(
        JKUnresolvedMethod(//TODO resolve assert
            "assert",
            kotlinTypeByName(KotlinBuiltIns.FQ_NAMES.unit.asString(), symbolProvider)
        ),
        JKExpressionListImpl(listOfNotNull(assertion, message))
    )

fun throwAnnotation(throws: List<JKType>, symbolProvider: JKSymbolProvider) =
    JKAnnotationImpl(
        symbolProvider.provideByFqName("kotlin.jvm.Throws"),
        JKExpressionListImpl(
            throws.map {
                JKClassLiteralExpressionImpl(JKTypeElementImpl(it), JKClassLiteralExpression.LiteralType.KOTLIN_CLASS)
            }
        )
    )
