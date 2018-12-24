/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.conversions.RecursiveApplicableConversionBase
import org.jetbrains.kotlin.j2k.conversions.multiResolveFqName
import org.jetbrains.kotlin.j2k.conversions.resolveFqName
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.math.BigInteger

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
            )
            psiClass?.let { klass ->
                symbolProvider.provideDirectSymbol(klass) as? JKClassSymbol
            }
        }
        else -> null
    }

private fun JKKtOperatorToken.binaryExpressionMethodSymbol(
    leftType: JKType,
    rightType: JKType,
    symbolProvider: JKSymbolProvider
): JKMethodSymbol {
    fun PsiClass.methodSymbol() =
        allMethods
            .filter { it.name == operatorName }
            .firstOrNull {
                it.parameterList.parameters.singleOrNull()?.takeIf { parameter ->
                    val type = parameter.type.toJK(symbolProvider)
                    if (type !is JKTypeParameterType) rightType.isSubtypeOf(type, symbolProvider)
                    else true//TODO check for type bounds
                } != null
            }?.let { symbolProvider.provideDirectSymbol(it) as JKMethodSymbol }


    val classSymbol =
        if (leftType.isStringType()) symbolProvider.provideByFqName(KotlinBuiltIns.FQ_NAMES.string.toSafe())
        else leftType.classSymbol(symbolProvider)

    val defaultClassSymbol by lazy {
        when (text) {
            "<", ">", "<=", ">=", "==", "!=", "&&", "||" ->
                JKUnresolvedMethod(
                    text,
                    kotlinTypeByName(
                        KotlinBuiltIns.FQ_NAMES._boolean.toSafe().asString(),
                        symbolProvider,
                        Nullability.NotNull
                    )
                )
            "+", "-", "*", "/" -> JKUnresolvedMethod(text, leftType)//TODO fix that
            else -> TODO()
        }
    }
    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->
            classSymbol.target.declarations
                .asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name == operatorName }
                .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
                .firstOrNull { it.parameterTypes?.singleOrNull()?.takeIf { rightType.isSubtypeOf(it, symbolProvider) } != null }
                ?: defaultClassSymbol
        is JKUniverseClassSymbol -> classSymbol.target.psi<PsiClass>()?.methodSymbol() ?: defaultClassSymbol
        is JKMultiverseClassSymbol -> classSymbol.target.methodSymbol() ?: defaultClassSymbol
        else -> defaultClassSymbol
    }
}

private fun JKKtOperatorToken.unaryExpressionMethodSymbol(
    operandType: JKType,
    symbolProvider: JKSymbolProvider
): JKMethodSymbol {
    if (this == KtTokens.EXCLEQ) {
        return JKExclExclMethod(operandType)
    }
    val classSymbol = operandType.classSymbol(symbolProvider)
    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->// todo look for extensions
            classSymbol.target.declarations.asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name == operatorName }
                .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
                .firstOrNull()!!
        null -> TODO(" No class symbol")
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

fun jvmAnnotation(name: String, symbolProvider: JKSymbolProvider) =
    JKAnnotationImpl(
        symbolProvider.provideByFqName("kotlin.annotation.AnnotationTarget.$name")
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

fun JKVariable.findUsages(scope: JKTreeElement, context: ConversionContext): List<JKFieldAccessExpression> {
    val symbol = context.symbolProvider.provideUniverseSymbol(this)
    val usages = mutableListOf<JKFieldAccessExpression>()
    val searcher = object : RecursiveApplicableConversionBase() {
        override fun applyToElement(element: JKTreeElement): JKTreeElement {
            if (element is JKExpression) {
                element.unboxFieldReference()?.also {
                    if (it.identifier == symbol) {
                        usages += it
                    }
                }
            }
            return recurse(element)
        }
    }
    searcher.runConversion(scope, context)
    return usages
}

fun JKExpression.unboxFieldReference(): JKFieldAccessExpression? = when {
    this is JKFieldAccessExpression -> this
    this is JKQualifiedExpression && receiver is JKThisExpression -> selector as? JKFieldAccessExpression
    else -> null
}

fun JKFieldAccessExpression.asAssignmentFromTarget(): JKKtAssignmentStatement? =
    (parent as? JKKtAssignmentStatement)
        ?.takeIf { it.field == this }

fun JKFieldAccessExpression.isInDecrementOrIncrement(): Boolean =
    (parent as? JKUnaryExpression)?.operator?.token?.text in listOf("++", "--")

fun JKExpression.bangedBangedExpr(context: ConversionContext): JKExpression =
    JKPostfixExpressionImpl(
        this,
        JKKtOperatorImpl(KtTokens.EXCLEXCL, JKExclExclMethod(type(context)!!))
    )

fun JKVariable.hasWritableUsages(scope: JKTreeElement, context: ConversionContext): Boolean =
    findUsages(scope, context).any {
        it.asAssignmentFromTarget() != null
                || it.isInDecrementOrIncrement()
    }

fun JKLiteralExpression.fixLiteral(expectedType: JKLiteralExpression.LiteralType): JKLiteralExpression =
    when (expectedType) {
        JKLiteralExpression.LiteralType.DOUBLE -> convertDoubleLiteral(literal)
        JKLiteralExpression.LiteralType.FLOAT -> convertFloatLiteral(literal)
        JKLiteralExpression.LiteralType.LONG, JKLiteralExpression.LiteralType.INT -> convertIntegerLiteral(this)
        JKLiteralExpression.LiteralType.CHAR -> convertCharLiteral(literal)
        JKLiteralExpression.LiteralType.STRING -> convertStringLiteral(literal)
        else -> this
    }


private fun convertDoubleLiteral(text: String): JKKtLiteralExpression {
    var newText =
        text.replace("L", "", true)
            .replace("d", "", true)
            .replace(".e", "e", true)
            .replace(".f", "", true)
            .replace("f", "", true)

    if (!newText.contains(".") && !newText.contains("e", true))
        newText += "."
    if (newText.endsWith("."))
        newText += "0"

    return JKKtLiteralExpressionImpl(
        newText,
        JKLiteralExpression.LiteralType.DOUBLE
    )
}

private fun convertFloatLiteral(text: String): JKKtLiteralExpressionImpl {
    return JKKtLiteralExpressionImpl(
        text.replace("L", "", true)
            .replace(".f", "f", true)
            .replace("F", "f")
            .replace(".e", "e", true)
            .let {
                if (!it.endsWith("f")) "${it}f"
                else it
            },
        JKLiteralExpression.LiteralType.FLOAT
    )
}

private fun convertStringLiteral(text: String): JKKtLiteralExpressionImpl {
    var newText = text.replace("((?:\\\\)*)\\\\([0-3]?[0-7]{1,2})".toRegex()) {
        val leadingBackslashes = it.groupValues[1]
        if (leadingBackslashes.length % 2 == 0) {
            String.format("%s\\u%04x", leadingBackslashes, Integer.parseInt(it.groupValues[2], 8))
        } else {
            it.value
        }
    }
    newText = newText.replace("\\$([A-Za-z]+|\\{)".toRegex(), "\\\\$0")

    return JKKtLiteralExpressionImpl(newText, JKLiteralExpression.LiteralType.STRING)
}

private fun convertCharLiteral(text: String): JKKtLiteralExpression {
    return JKKtLiteralExpressionImpl(
        text.replace("\\\\([0-3]?[0-7]{1,2})".toRegex()) {
            String.format("\\u%04x", Integer.parseInt(it.groupValues[1], 8))
        },
        JKLiteralExpression.LiteralType.CHAR
    )
}

private fun convertIntegerLiteral(element: JKLiteralExpression): JKKtLiteralExpression {
    var text = element.literal
    if (element.type == JKLiteralExpression.LiteralType.LONG) {
        text = text.replace("l", "L")
    }

    fun isHexLiteral(text: String) = text.startsWith("0x") || text.startsWith("0X")

    if ((element.type == JKLiteralExpression.LiteralType.LONG || element.type == JKLiteralExpression.LiteralType.INT) && isHexLiteral(text)) {
        val v = BigInteger(text.substring(2).replace("L", ""), 16)
        if (text.contains("L")) {
            if (v.bitLength() > 63) {
                text = "-0x${v.toLong().toString(16).substring(1)}L"
            }
        } else {
            if (v.bitLength() > 31) {
                text = "-0x${v.toInt().toString(16).substring(1)}"
            }
        }
    } else if (element.type == JKLiteralExpression.LiteralType.INT) {
        text = element.literal
    }

    return JKKtLiteralExpressionImpl(
        text,
        element.type
    )
}