/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.PsiClass
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.conversions.RecursiveApplicableConversionBase
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.math.BigInteger

fun kotlinTypeByName(name: String, symbolProvider: JKSymbolProvider, nullability: Nullability = Nullability.Nullable): JKClassType =
    JKClassTypeImpl(
        symbolProvider.provideByFqName(name),
        emptyList(),
        nullability
    )

private fun JKType.classSymbol(symbolProvider: JKSymbolProvider) =
    when (this) {
        is JKClassType -> classReference
        is JKJavaPrimitiveType ->
            symbolProvider.provideByFqName(jvmPrimitiveType.primitiveType.typeFqName.asString())

        else -> null
    }

private fun JKKtOperatorToken.arithmeticMethodType(
    leftType: JKType,
    rightType: JKType,
    symbolProvider: JKSymbolProvider
): JKType? {
    fun PsiClass.methodReturnType() =
        allMethods
            .filter { it.name == operatorName }
            .firstOrNull {
                it.parameterList.parameters.singleOrNull()?.takeIf { parameter ->
                    val type = parameter.type.toJK(symbolProvider)
                    if (type !is JKTypeParameterType) rightType.isSubtypeOf(type, symbolProvider)
                    else true//TODO check for type bounds
                } != null
            }?.let { it.returnType?.toJK(symbolProvider) }


    val classSymbol =
        if (leftType.isStringType()) symbolProvider.provideByFqName(KotlinBuiltIns.FQ_NAMES.string.toSafe())
        else leftType.classSymbol(symbolProvider)

    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->
            classSymbol.target.declarations
                .asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name == operatorName }
                .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
                .firstOrNull { it.parameterTypes?.singleOrNull()?.takeIf { rightType.isSubtypeOf(it, symbolProvider) } != null }
                ?.returnType
        is JKUniverseClassSymbol -> classSymbol.target.psi<PsiClass>()?.methodReturnType()
        is JKMultiverseClassSymbol -> classSymbol.target.methodReturnType()
        else -> null
    }
}

private fun JKKtOperatorToken.unaryExpressionMethodType(
    operandType: JKType?,
    symbolProvider: JKSymbolProvider
): JKType {
    if (operandType is JKNoType) {
        return operandType
    }
    if (this == KtTokens.EXCLEXCL) {
        return operandType!!
    }
    if (this == KtTokens.EXCL) {
        return symbolProvider.provideByFqName<JKClassSymbol>(KotlinBuiltIns.FQ_NAMES._boolean).asType()
    }
    if (this == KtTokens.MINUS || this == KtTokens.PLUS) {
        return operandType!!
    }
    if (this == KtTokens.EXCL) {
        return operandType!!
    }
    val classSymbol = operandType!!.classSymbol(symbolProvider)
    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->// todo look for extensions
            classSymbol.target.declarations.asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name == operatorName }
                .mapNotNull { it.typeReference?.toJK(symbolProvider) }
                .firstOrNull() ?: TODO(classSymbol::class.toString() + this.operatorName)
        null -> TODO(" No class symbol")
        else -> TODO(classSymbol::class.toString())
    }
}

fun JKOperator.isComparationOperator() =
    (token as? JKKtSingleValueOperatorToken)?.psiToken in comparationOperators

fun JKOperator.isEquals() =
    (token as? JKKtSingleValueOperatorToken)?.psiToken in equalsOperators

fun JKOperator.isArithmetic() =
    (token as? JKKtSingleValueOperatorToken)?.psiToken in arithmeticOperators

fun JKOperator.isLessOrGreater() =
    (token as? JKKtSingleValueOperatorToken)?.psiToken in lessGreaterOperators

private val equalsOperators =
    TokenSet.create(
        KtTokens.EQEQEQ,
        KtTokens.EXCLEQEQEQ,
        KtTokens.EQEQ,
        KtTokens.EXCLEQ
    )

private val lessGreaterOperators =
    TokenSet.create(
        KtTokens.LT,
        KtTokens.GT,
        KtTokens.LTEQ,
        KtTokens.GTEQ
    )

private val comparationOperators =
    TokenSet.orSet(
        lessGreaterOperators,
        equalsOperators
    )

private val booleanOperators =
    TokenSet.orSet(
        comparationOperators,
        TokenSet.create(
            KtTokens.ANDAND,
            KtTokens.OROR
        )
    )

private val arithmeticOperators = TokenSet.create(
    KtTokens.MUL,
    KtTokens.PLUS,
    KtTokens.MINUS,
    KtTokens.DIV,
    KtTokens.PERC
)

private fun JKKtOperatorToken.defaultReturnType(leftType: JKType?, rightType: JKType?, symbolProvider: JKSymbolProvider): JKType? {
    if (this is JKKtSingleValueOperatorToken && psiToken in arithmeticOperators) return leftType
    return null
}

fun kotlinBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    token: JKKtOperatorToken,
    symbolProvider: JKSymbolProvider
): JKBinaryExpression {
    val returnType =
        when {
            token is JKKtSingleValueOperatorToken && token.psiToken in booleanOperators ->
                JKClassTypeImpl(symbolProvider.provideByFqName(KotlinBuiltIns.FQ_NAMES._boolean))
            else -> {
                val leftType = left.type(symbolProvider)
                val rightType = right.type(symbolProvider)
                leftType?.let { l ->
                    rightType?.let { r ->
                        token.arithmeticMethodType(l, r, symbolProvider)
                    }
                } ?: token.defaultReturnType(leftType, rightType, symbolProvider)
                ?: symbolProvider.provideByFqName<JKClassSymbol>(KotlinBuiltIns.FQ_NAMES.nothing).asType()
            }
        }
    return JKBinaryExpressionImpl(left, right, JKKtOperatorImpl(token, returnType))
}

fun kotlinBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    token: KtSingleValueToken,
    symbolProvider: JKSymbolProvider
): JKBinaryExpression =
    kotlinBinaryExpression(
        left,
        right,
        JKKtSingleValueOperatorToken(token),
        symbolProvider
    )

fun kotlinPrefixExpression(
    operand: JKExpression,
    token: JKKtOperatorToken,
    symbolProvider: JKSymbolProvider
): JKPrefixExpression {
    val operandType = operand.type(symbolProvider)
    val methodSymbol = token.unaryExpressionMethodType(operandType, symbolProvider)
    return JKPrefixExpressionImpl(operand, JKKtOperatorImpl(token, methodSymbol))
}

fun kotlinPostfixExpression(
    operand: JKExpression,
    token: JKKtOperatorToken,
    symbolProvider: JKSymbolProvider
): JKPostfixExpression {
    val operandType = operand.type(symbolProvider)
    val methodSymbol = token.unaryExpressionMethodType(operandType, symbolProvider)
    return JKPostfixExpressionImpl(operand, JKKtOperatorImpl(token, methodSymbol))
}

fun untilToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: ConversionContext
): JKExpression =
    rangeExpression(
        from,
        to,
        "until",
        conversionContext
    )

fun downToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: ConversionContext
): JKExpression =
    rangeExpression(
        from,
        to,
        "downTo",
        conversionContext
    )

fun List<JKExpression>.toExpressionList() =
    JKExpressionListImpl(this)

fun JKExpression.parenthesizeIfBinaryExpression() =
    when (this) {
        is JKBinaryExpression -> JKParenthesizedExpressionImpl(this)
        else -> this
    }

fun rangeExpression(
    from: JKExpression,
    to: JKExpression,
    operatorName: String,
    conversionContext: ConversionContext
): JKExpression =
    JKBinaryExpressionImpl(
        from,
        to,
        JKKtOperatorImpl(
            JKKtWordOperatorToken(operatorName),
            conversionContext.symbolProvider.provideByFqName<JKMethodSymbol>("kotlin.ranges.$operatorName").returnType!!
        )
    )


fun blockStatement(vararg statements: JKStatement) =
    JKBlockStatementImpl(JKBlockImpl(statements.toList()))

fun blockStatement(statements: List<JKStatement>) =
    JKBlockStatementImpl(JKBlockImpl(statements))

fun useExpression(
    receiver: JKExpression,
    variableIdentifier: JKNameIdentifier,
    body: JKStatement,
    symbolProvider: JKSymbolProvider
): JKExpression {
    val useSymbol = symbolProvider.provideByFqName<JKMethodSymbol>("kotlin.io.use")
    val lambdaParameter =
        JKParameterImpl(JKTypeElementImpl(JKNoTypeImpl), variableIdentifier)

    val lambda = JKLambdaExpressionImpl(
        body,
        listOf(lambdaParameter)
    )
    val methodCall =
        JKJavaMethodCallExpressionImpl(
            useSymbol,
            listOf(lambda).toArgumentList()
        )
    return JKQualifiedExpressionImpl(receiver, JKKtQualifierImpl.DOT, methodCall)
}

fun kotlinAssert(assertion: JKExpression, message: JKExpression?, symbolProvider: JKSymbolProvider) =
    JKKtCallExpressionImpl(
        JKUnresolvedMethod(//TODO resolve assert
            "assert",
            kotlinTypeByName(KotlinBuiltIns.FQ_NAMES.unit.asString(), symbolProvider)
        ),
        (listOfNotNull(assertion, message)).toArgumentList()
    )

fun jvmAnnotation(name: String, symbolProvider: JKSymbolProvider) =
    JKAnnotationImpl(
        symbolProvider.provideByFqName("kotlin.jvm.$name")
    )

fun throwAnnotation(throws: List<JKType>, symbolProvider: JKSymbolProvider) =
    JKAnnotationImpl(
        symbolProvider.provideByFqName("kotlin.jvm.Throws"),
        throws.map {
            JKAnnotationParameterImpl(
                JKClassLiteralExpressionImpl(JKTypeElementImpl(it), JKClassLiteralExpression.LiteralType.KOTLIN_CLASS)
            )
        }
    )

fun JKAnnotationList.annotationByFqName(fqName: String): JKAnnotation? =
    annotations.firstOrNull { it.classSymbol.fqName == fqName }

fun stringLiteral(content: String, symbolProvider: JKSymbolProvider): JKExpression {
    val lines = content.split('\n')
    return lines.mapIndexed { i, line ->
        val newlineSeparator = if (i == lines.size - 1) "" else "\\n"
        JKKtLiteralExpressionImpl("\"$line$newlineSeparator\"", JKLiteralExpression.LiteralType.STRING)
    }.reduce { acc: JKExpression, literalExpression: JKKtLiteralExpression ->
        kotlinBinaryExpression(acc, literalExpression, JKKtSingleValueOperatorToken(KtTokens.PLUS), symbolProvider)!!
    }
}

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

fun JKExpression.bangedBangedExpr(symbolProvider: JKSymbolProvider): JKExpression =
    JKPostfixExpressionImpl(
        this,
        JKKtOperatorImpl(KtTokens.EXCLEXCL, type(symbolProvider)!!)
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
    }.withNonCodeElementsFrom(this)

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
        text = text.replace("l", "L").let {
            if (!it.endsWith("L")) it + "L" else it
        }
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

fun equalsExpression(left: JKExpression, right: JKExpression, symbolProvider: JKSymbolProvider) =
    kotlinBinaryExpression(
        left,
        right,
        KtTokens.EQEQ,
        symbolProvider
    )

fun createCompanion(declarations: List<JKDeclaration>): JKClass =
    JKClassImpl(
        JKNameIdentifierImpl(""),
        JKInheritanceInfoImpl(emptyList(), emptyList()),
        JKClass.ClassKind.COMPANION,
        JKTypeParameterListImpl(),
        JKClassBodyImpl(declarations),
        JKAnnotationListImpl(),
        emptyList(),
        JKVisibilityModifierElementImpl(Visibility.PUBLIC),
        JKModalityModifierElementImpl(Modality.FINAL)
    )

fun JKClass.getCompanion(): JKClass? =
    declarationList.firstOrNull { it is JKClass && it.classKind == JKClass.ClassKind.COMPANION } as? JKClass

fun JKClass.getOrCreateCompainonObject(): JKClass =
    getCompanion()
        ?: JKClassImpl(
            JKNameIdentifierImpl(""),
            JKInheritanceInfoImpl(emptyList(), emptyList()),
            JKClass.ClassKind.COMPANION,
            JKTypeParameterListImpl(),
            JKClassBodyImpl(),
            JKAnnotationListImpl(),
            emptyList(),
            JKVisibilityModifierElementImpl(Visibility.PUBLIC),
            JKModalityModifierElementImpl(Modality.FINAL)
        ).also { classBody.declarations += it }

fun runExpression(body: JKStatement, symbolProvider: JKSymbolProvider): JKExpression {
    val lambda = JKLambdaExpressionImpl(
        body,
        emptyList()
    )
    return JKKtCallExpressionImpl(
        symbolProvider.provideByFqNameMulti("kotlin.run"),
        (listOf(lambda)).toArgumentList()
    )
}

fun JKTreeElement.asQualifierWithThisAsSelector(): JKQualifiedExpression? =
    parent?.safeAs<JKQualifiedExpression>()
        ?.takeIf { it.selector == this }

fun JKAnnotationMemberValue.toExpression(symbolProvider: JKSymbolProvider): JKExpression {
    fun handleAnnotationParameter(element: JKTreeElement): JKTreeElement =
        when (element) {
            is JKClassLiteralExpression ->
                element.also {
                    element.literalType = JKClassLiteralExpression.LiteralType.KOTLIN_CLASS
                }
            is JKTypeElement ->
                JKTypeElementImpl(element.type.replaceJavaClassWithKotlinClassType(symbolProvider))
            else -> applyRecursive(element, ::handleAnnotationParameter)
        }

    return handleAnnotationParameter(
        when {
            this is JKStubExpression -> this
            this is JKAnnotation ->
                JKJavaNewExpressionImpl(
                    classSymbol,
                    JKArgumentListImpl(
                        arguments.map { argument ->
                            val value = argument.value.copyTreeAndDetach().toExpression(symbolProvider)
                            when (argument) {
                                is JKAnnotationNameParameter ->
                                    JKNamedArgumentImpl(value, JKNameIdentifierImpl(argument.name.value))
                                else -> JKArgumentImpl(value)
                            }

                        }
                    ),
                    JKTypeArgumentListImpl()
                )
            this is JKKtAnnotationArrayInitializerExpression ->
                JKKtAnnotationArrayInitializerExpressionImpl(initializers.map { it.detached(this).toExpression(symbolProvider) })
            this is JKExpression -> this
            else -> error("Bad initializer")
        }
    ) as JKExpression
}


fun JKExpression.asLiteralTextWithPrefix(): String? =
    when {
        this is JKPrefixExpression
                && (operator.token.text == "+" || operator.token.text == "-")
                && expression is JKLiteralExpression
        -> operator.token.text + expression.cast<JKLiteralExpression>().literal
        this is JKLiteralExpression -> literal
        else -> null
    }

inline fun JKClass.primaryConstructor(): JKKtPrimaryConstructor? =
    classBody.declarations.firstIsInstanceOrNull()

fun List<JKExpression>.toArgumentList(): JKArgumentList =
    JKArgumentListImpl(map { JKArgumentImpl(it) })

fun JKAnnotation.isVarargsArgument(index: Int): Boolean {
    val target = classSymbol.target
    return when (target) {
        is JKClass -> target.primaryConstructor()?.parameters?.getOrNull(index)?.isVarArgs
        is PsiClass -> target.methods.getOrNull(index)?.let {
            it.isVarArgs || it.name == "value"
        }
        else -> false
    } ?: false
}


fun JKExpression.asStatement(): JKExpressionStatement =
    JKExpressionStatementImpl(this)

