/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.PsiClass
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.conversions.RecursiveApplicableConversionBase
import org.jetbrains.kotlin.nj2k.symbols.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.types.JKClassType
import org.jetbrains.kotlin.nj2k.types.JKJavaPrimitiveType
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.nj2k.types.JKTypeParameterType
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun JKType.classSymbol(typeFactory: JKTypeFactory) = when (this) {
    is JKClassType -> classReference
    is JKJavaPrimitiveType -> typeFactory.fromPrimitiveType(this).classReference
    else -> null
}

private fun JKKtOperatorToken.arithmeticMethodType(
    leftType: JKType,
    rightType: JKType,
    typeFactory: JKTypeFactory
): JKType? {
    val symbolProvider = typeFactory.symbolProvider

    fun PsiClass.methodReturnType() =
        allMethods
            .filter { it.name == operatorName }
            .firstOrNull {
                it.parameterList.parameters.singleOrNull()?.takeIf { parameter ->
                    val type = typeFactory.fromPsiType(parameter.type)
                    if (type !is JKTypeParameterType) rightType.isSubtypeOf(type, typeFactory)
                    else true//TODO check for type bounds
                } != null
            }?.returnType?.let { typeFactory.fromPsiType(it) }

    val classSymbol =
        if (leftType.isStringType()) symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.string.toSafe())
        else leftType.classSymbol(typeFactory)

    return when (classSymbol) {
        is JKMultiverseKtClassSymbol ->
            classSymbol.target.declarations
                .asSequence()
                .filterIsInstance<KtNamedFunction>()
                .filter { it.name == operatorName }
                .mapNotNull { symbolProvider.provideDirectSymbol(it) as? JKMethodSymbol }
                .firstOrNull { it.parameterTypes?.singleOrNull()?.takeIf { rightType.isSubtypeOf(it, typeFactory) } != null }
                ?.returnType
        is JKUniverseClassSymbol -> classSymbol.target.psi<PsiClass>()?.methodReturnType()
        is JKMultiverseClassSymbol -> classSymbol.target.methodReturnType()
        else -> null
    }
}

fun JKOperator.isEquals() =
    (token as? JKKtSingleValueOperatorToken)?.psiToken in equalsOperators

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

private val comparisonOperators =
    TokenSet.orSet(
        lessGreaterOperators,
        equalsOperators
    )

private val booleanOperators =
    TokenSet.orSet(
        comparisonOperators,
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

private fun JKKtOperatorToken.defaultReturnType(leftType: JKType?): JKType? {
    if (this is JKKtSingleValueOperatorToken && psiToken in arithmeticOperators) return leftType
    return null
}

fun kotlinBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    token: JKKtOperatorToken,
    typeFactory: JKTypeFactory
): JKBinaryExpression {
    val symbolProvider = typeFactory.symbolProvider
    val returnType =
        when {
            token is JKKtSingleValueOperatorToken && token.psiToken in booleanOperators ->
                JKClassTypeImpl(symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES._boolean.toSafe()))
            else -> {
                val leftType = left.type(typeFactory)
                val rightType = right.type(typeFactory)
                leftType?.let { l ->
                    rightType?.let { r ->
                        token.arithmeticMethodType(l, r, typeFactory)
                    }
                } ?: token.defaultReturnType(leftType)
                ?: typeFactory.types.nothing
            }
        }
    return JKBinaryExpressionImpl(left, right, JKKtOperatorImpl(token, returnType))
}

fun kotlinBinaryExpression(
    left: JKExpression,
    right: JKExpression,
    token: KtSingleValueToken,
    typeFactory: JKTypeFactory
): JKBinaryExpression =
    kotlinBinaryExpression(
        left,
        right,
        JKKtSingleValueOperatorToken(token),
        typeFactory
    )

fun kotlinPrefixExpression(
    operand: JKExpression,
    token: JKKtOperatorToken,
    typeFactory: JKTypeFactory
): JKPrefixExpression {
    val operandType = operand.type(typeFactory) ?: JKNoTypeImpl
    return JKPrefixExpressionImpl(operand, JKKtOperatorImpl(token, operandType))
}

fun kotlinPostfixExpression(
    operand: JKExpression,
    token: JKKtOperatorToken,
    typeFactory: JKTypeFactory
): JKPostfixExpression {
    val operandType = operand.type(typeFactory) ?: JKNoTypeImpl
    return JKPostfixExpressionImpl(operand, JKKtOperatorImpl(token, operandType))
}

fun untilToExpression(
    from: JKExpression,
    to: JKExpression,
    conversionContext: NewJ2kConverterContext
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
    conversionContext: NewJ2kConverterContext
): JKExpression =
    rangeExpression(
        from,
        to,
        "downTo",
        conversionContext
    )

fun JKExpression.parenthesizeIfBinaryExpression() =
    when (this) {
        is JKBinaryExpression -> JKParenthesizedExpressionImpl(this)
        else -> this
    }

fun rangeExpression(
    from: JKExpression,
    to: JKExpression,
    operatorName: String,
    conversionContext: NewJ2kConverterContext
): JKExpression =
    JKBinaryExpressionImpl(
        from,
        to,
        JKKtOperatorImpl(
            JKKtWordOperatorToken(operatorName),
            conversionContext.symbolProvider.provideMethodSymbol("kotlin.ranges.$operatorName").returnType!!
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
    val useSymbol = symbolProvider.provideMethodSymbol("kotlin.io.use")
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

fun kotlinAssert(assertion: JKExpression, message: JKExpression?, typeFactory: JKTypeFactory) =
    JKKtCallExpressionImpl(
        JKUnresolvedMethod(//TODO resolve assert
            "assert",
            typeFactory,
            typeFactory.types.unit
        ),
        (listOfNotNull(assertion, message)).toArgumentList()
    )

fun jvmAnnotation(name: String, symbolProvider: JKSymbolProvider) =
    JKAnnotationImpl(
        symbolProvider.provideClassSymbol("kotlin.jvm.$name")
    )

fun throwAnnotation(throws: List<JKType>, symbolProvider: JKSymbolProvider) =
    JKAnnotationImpl(
        symbolProvider.provideClassSymbol("kotlin.jvm.Throws"),
        throws.map {
            JKAnnotationParameterImpl(
                JKClassLiteralExpressionImpl(JKTypeElementImpl(it), JKClassLiteralExpression.LiteralType.KOTLIN_CLASS)
            )
        }
    )

fun JKAnnotationList.annotationByFqName(fqName: String): JKAnnotation? =
    annotations.firstOrNull { it.classSymbol.fqName == fqName }

fun stringLiteral(content: String, typeFactory: JKTypeFactory): JKExpression {
    val lines = content.split('\n')
    return lines.mapIndexed { i, line ->
        val newlineSeparator = if (i == lines.size - 1) "" else "\\n"
        JKKtLiteralExpressionImpl("\"$line$newlineSeparator\"", JKLiteralExpression.LiteralType.STRING)
    }.reduce { acc: JKExpression, literalExpression: JKKtLiteralExpression ->
        kotlinBinaryExpression(acc, literalExpression, JKKtSingleValueOperatorToken(KtTokens.PLUS), typeFactory)
    }
}

fun JKVariable.findUsages(scope: JKTreeElement, context: NewJ2kConverterContext): List<JKFieldAccessExpression> {
    val symbol = context.symbolProvider.provideUniverseSymbol(this)
    val usages = mutableListOf<JKFieldAccessExpression>()
    val searcher = object : RecursiveApplicableConversionBase(context) {
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

fun JKVariable.hasWritableUsages(scope: JKTreeElement, context: NewJ2kConverterContext): Boolean =
    findUsages(scope, context).any {
        it.asAssignmentFromTarget() != null
                || it.isInDecrementOrIncrement()
    }

fun equalsExpression(left: JKExpression, right: JKExpression, typeFactory: JKTypeFactory) =
    kotlinBinaryExpression(
        left,
        right,
        KtTokens.EQEQ,
        typeFactory
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

fun JKClass.getOrCreateCompanionObject(): JKClass =
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
        symbolProvider.provideMethodSymbol("kotlin.run"),
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

fun JKClass.primaryConstructor(): JKKtPrimaryConstructor? = classBody.declarations.firstIsInstanceOrNull()

fun List<JKExpression>.toArgumentList(): JKArgumentList =
    JKArgumentListImpl(map { JKArgumentImpl(it) })


fun JKExpression.asStatement(): JKExpressionStatement =
    JKExpressionStatementImpl(this)

fun <T : JKExpression> T.nullIfStubExpression(): T? =
    if (this is JKStubExpression) null
    else this

fun JKExpression.qualified(qualifier: JKExpression?) =
    if (qualifier != null && qualifier !is JKStubExpression) {
        JKQualifiedExpressionImpl(qualifier, JKJavaQualifierImpl.DOT, this)
    } else this