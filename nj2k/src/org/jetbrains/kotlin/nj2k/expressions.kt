/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.conversions.RecursiveApplicableConversionBase
import org.jetbrains.kotlin.nj2k.symbols.JKMethodSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUnresolvedMethod
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun JKOperator.isEquals() =
    (token as? JKKtSingleValueOperatorToken)?.psiToken in equalsOperators

private val equalsOperators =
    TokenSet.create(
        KtTokens.EQEQEQ,
        KtTokens.EXCLEQEQEQ,
        KtTokens.EQEQ,
        KtTokens.EXCLEQ
    )

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

fun JKExpression.parenthesize() = JKParenthesizedExpressionImpl(this)

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
        JKBinaryExpressionImpl(acc, literalExpression, JKKtOperatorImpl(JKOperatorToken.PLUS, typeFactory.types.string))
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
    JKBinaryExpressionImpl(
        left,
        right,
        JKKtOperatorImpl(
            JKOperatorToken.EQEQ,
            typeFactory.types.boolean
        )
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

fun JKExpression.callOn(
    symbol: JKMethodSymbol,
    arguments: List<JKExpression> = emptyList(),
    typeArguments: List<JKTypeElement> = emptyList()
) = JKQualifiedExpressionImpl(
    this,
    JKKtQualifierImpl.DOT,
    JKKtCallExpressionImpl(
        symbol,
        JKArgumentListImpl(arguments.map { JKArgumentImpl(it) }),
        JKTypeArgumentListImpl(typeArguments)
    )
)

