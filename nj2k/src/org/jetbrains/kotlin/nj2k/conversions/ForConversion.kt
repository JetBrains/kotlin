/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.j2k.ReferenceSearcher
import org.jetbrains.kotlin.j2k.hasWriteAccesses
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.symbols.deepestFqName
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import kotlin.math.abs


class ForConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    private val referenceSearcher: ReferenceSearcher
        get() = context.converter.converterServices.oldServices.referenceSearcher

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaForLoopStatement) return recurse(element)

        convertToForeach(element)?.also { return recurse(it.withNonCodeElementsFrom(element)) }
        convertToWhile(element)?.also { return recurse(it.withNonCodeElementsFrom(element)) }

        return recurse(element)
    }

    private fun convertToWhile(loopStatement: JKJavaForLoopStatement): JKStatement? {
        val whileBody = createWhileBody(loopStatement)
        val condition =
            if (loopStatement.condition !is JKStubExpression) loopStatement::condition.detached()
            else JKBooleanLiteral(true)
        val whileStatement = JKWhileStatementImpl(condition, whileBody)

        if (loopStatement.initializer is JKEmptyStatement) return whileStatement

        val convertedFromForLoopSyntheticWhileStatement =
            JKKtConvertedFromForLoopSyntheticWhileStatementImpl(
                loopStatement::initializer.detached(),
                whileStatement
            )

        val notNeedParentBlock = loopStatement.parent is JKBlock
                || loopStatement.parent is JKLabeledStatement && loopStatement.parent?.parent is JKBlock

        return when {
            loopStatement.hasNameConflict() ->
                JKExpressionStatementImpl(
                    runExpression(
                        convertedFromForLoopSyntheticWhileStatement,
                        context.symbolProvider
                    )
                )
            !notNeedParentBlock -> blockStatement(convertedFromForLoopSyntheticWhileStatement)
            else -> convertedFromForLoopSyntheticWhileStatement
        }
    }

    private fun createWhileBody(loopStatement: JKJavaForLoopStatement): JKStatement {
        if (loopStatement.updaters.singleOrNull() is JKEmptyStatement) return loopStatement::body.detached()
        val continueStatementConverter = object : RecursiveApplicableConversionBase() {
            override fun applyToElement(element: JKTreeElement): JKTreeElement {
                if (element !is JKContinueStatement) return recurse(element)
                val elementPsi = element.psi<PsiContinueStatement>()!!
                if (elementPsi.findContinuedStatement()?.toContinuedLoop() != loopStatement.psi<PsiForStatement>()) return recurse(element)
                val statements = loopStatement.updaters.map { it.copyTreeAndDetach() } + element.copyTreeAndDetach()
                return if (element.parent is JKBlock)
                    JKBlockStatementWithoutBracketsImpl(statements)
                else JKBlockStatementImpl(JKBlockImpl(statements))
            }
        }

        val body = continueStatementConverter.applyToElement(loopStatement::body.detached())

        if (body is JKBlockStatement) {
            val initializer = loopStatement.initializer
            val hasNameConflict =
                initializer is JKDeclarationStatement && initializer.declaredStatements.any { loopVar ->
                    loopVar is JKLocalVariable && body.statements.any { statement ->
                        statement is JKDeclarationStatement && statement.declaredStatements.any {
                            it is JKLocalVariable && it.name.value == loopVar.name.value
                        }
                    }
                }

            val statements =
                if (hasNameConflict) {
                    listOf(JKExpressionStatementImpl(runExpression(body, context.symbolProvider))) + loopStatement::updaters.detached()
                } else {
                    body.block::statements.detached() + loopStatement::updaters.detached()
                }
            return JKBlockStatementImpl(JKBlockImpl(statements))
        } else {
            val statements =
                listOf(body as JKStatement) + loopStatement::updaters.detached()
            return JKBlockStatementImpl(JKBlockImpl(statements))
        }
    }

    private fun convertToForeach(loopStatement: JKJavaForLoopStatement): JKForInStatement? {
        val loopVar =
            (loopStatement.initializer as? JKDeclarationStatement)?.declaredStatements?.singleOrNull() as? JKLocalVariable ?: return null
        val loopVarPsi = loopVar.psi<PsiLocalVariable>() ?: return null
        val condition = loopStatement.condition as? JKBinaryExpression ?: return null
        if (!loopVarPsi.hasWriteAccesses(referenceSearcher, loopStatement.body.psi())
            && !loopVarPsi.hasWriteAccesses(referenceSearcher, loopStatement.condition.psi())
        ) {
            val left = condition.left as? JKFieldAccessExpression ?: return null
            val right = condition::right.detached()
            if (right.psi<PsiExpression>()?.type in listOf(PsiType.DOUBLE, PsiType.FLOAT, PsiType.CHAR)) return null
            if (left.identifier.target != loopVar) return null
            val start = loopVar::initializer.detached()
            val operationType =
                (loopStatement.updaters.singleOrNull() as? JKExpressionStatement)?.expression?.isVariableIncrementOrDecrement(loopVar)
            val reversed = when (operationType?.token?.text) {
                "++" -> false
                "--" -> true
                else -> return null
            }
            val operatorToken =
                ((condition.operator as? JKKtOperatorImpl)?.token as? JKKtSingleValueOperatorToken)?.psiToken
            val inclusive = when (operatorToken) {
                KtTokens.LT -> if (reversed) return null else false
                KtTokens.LTEQ -> if (reversed) return null else true
                KtTokens.GT -> if (reversed) false else return null
                KtTokens.GTEQ -> if (reversed) true else return null
                KtTokens.EXCLEQ -> false
                else -> return null
            }
            val range = forIterationRange(start, right, reversed, inclusive, loopVarPsi)
            val explicitType =
                if (context.converter.settings.specifyLocalVariableTypeByDefault)
                    JKJavaPrimitiveTypeImpl.INT
                else JKNoTypeImpl
            val loopVarDeclaration =
                JKForLoopVariableImpl(
                    JKTypeElementImpl(explicitType),
                    loopVar::name.detached(),
                    JKStubExpressionImpl()
                )
            return JKForInStatementImpl(
                loopVarDeclaration,
                range,
                loopStatement::body.detached()
            )

        }
        return null
    }

    private fun PsiStatement.toContinuedLoop(): PsiLoopStatement? {
        return when (this) {
            is PsiLoopStatement -> this
            is PsiLabeledStatement -> statement?.toContinuedLoop()
            else -> null
        }
    }

    private fun forIterationRange(
        start: JKExpression,
        bound: JKExpression,
        reversed: Boolean,
        inclusiveComparison: Boolean,
        psiContext: PsiElement
    ): JKExpression {
        indicesIterationRange(start, bound, reversed, inclusiveComparison)?.also { return it }
        return when {
            reversed -> downToExpression(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else +1),
                context
            )
            bound !is JKKtLiteralExpression && !inclusiveComparison ->
                untilToExpression(
                    start,
                    convertBound(bound, 0),
                    context
                )
            else -> kotlinBinaryExpression(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else -1),
                JKKtSingleValueOperatorToken(KtTokens.RANGE),
                context.symbolProvider
            )
        }
    }

    private fun convertBound(bound: JKExpression, correction: Int): JKExpression {
        if (correction == 0) return bound

        if (bound is JKLiteralExpression && bound.type == JKLiteralExpression.LiteralType.INT) {
            val value = bound.literal.toInt()
            return JKKtLiteralExpressionImpl((value + correction).toString(), bound.type)
        }

        val sign = if (correction > 0) KtTokens.PLUS else KtTokens.MINUS
        return kotlinBinaryExpression(
            bound,
            JKKtLiteralExpressionImpl(abs(correction).toString(), JKLiteralExpression.LiteralType.INT),
            JKKtSingleValueOperatorToken(sign),
            context.symbolProvider
        )
    }

    private fun indicesIterationRange(
        start: JKExpression,
        bound: JKExpression,
        reversed: Boolean,
        inclusiveComparison: Boolean
    ): JKExpression? {
        val collectionSizeExpression =
            if (reversed) {
                if (!inclusiveComparison) return null

                if ((bound as? JKLiteralExpression)?.literal?.toIntOrNull() != 0) return null

                if (start !is JKBinaryExpression) return null
                if (start.operator.token.text != "-") return null
                if ((start.right as? JKLiteralExpression)?.literal?.toIntOrNull() != 1) return null
                start.left
            } else {
                if (inclusiveComparison) return null
                if ((start as? JKLiteralExpression)?.literal?.toIntOrNull() != 0) return null
                bound
            } as? JKQualifiedExpression ?: return null

        val indices = indicesByCollectionSize(collectionSizeExpression)
            ?: indicesByArrayLength(collectionSizeExpression)
            ?: return null

        return if (reversed) {
            JKQualifiedExpressionImpl(
                indices,
                JKKtQualifierImpl.DOT,
                JKJavaMethodCallExpressionImpl(
                    context.symbolProvider.provideMethodSymbol("kotlin.collections.reversed"),
                    JKArgumentListImpl()
                )
            )
        } else indices
    }


    private fun indicesByCollectionSize(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKMethodCallExpression ?: return null
        return if (methodCall.identifier.deepestFqName() == "java.util.Collection.size"
            && methodCall.arguments.arguments.isEmpty()
        ) toIndicesCall(javaSizeCall) else null
    }

    private fun indicesByArrayLength(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKFieldAccessExpression ?: return null
        val receiverType = javaSizeCall.receiver.type(context.symbolProvider)
        if (methodCall.identifier.name == "length" && receiverType is JKJavaArrayType) {
            return toIndicesCall(javaSizeCall)
        }
        return null
    }

    private fun toIndicesCall(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        if (javaSizeCall.psi == null) return null
        val selector = JKFieldAccessExpressionImpl(
            context.symbolProvider.provideFieldSymbol("kotlin.collections.indices")
        )
        return JKQualifiedExpressionImpl(javaSizeCall::receiver.detached(), javaSizeCall.operator, selector)
    }

    private fun JKJavaForLoopStatement.hasNameConflict(): Boolean {
        val names = initializer.declaredVariableNames()
        if (names.isEmpty()) return false

        val factory = PsiElementFactory.SERVICE.getInstance(context.project)
        for (name in names) {
            val refExpr = try {
                factory.createExpressionFromText(name, psi) as? PsiReferenceExpression ?: return true
            } catch (e: IncorrectOperationException) {
                return true
            }
            if (refExpr.resolve() != null) return true
        }

        return (parent as? JKBlock)
            ?.statements
            ?.takeLastWhile { it != this }
            ?.any {
                it.declaredVariableNames().any { it in names }
            } == true
    }

    private fun JKStatement.declaredVariableNames(): Collection<String> =
        when (this) {
            is JKDeclarationStatement ->
                declaredStatements.filterIsInstance<JKVariable>().map { it.name.value }
            is JKJavaForLoopStatement -> initializer.declaredVariableNames()
            else -> emptyList()
        }


    private fun JKExpression.isVariableIncrementOrDecrement(variable: JKLocalVariable): JKOperator? {
        val pair = when (this) {
            is JKPostfixExpression -> operator to expression
            is JKPrefixExpression -> operator to expression
            else -> return null
        }
        if ((pair.second as? JKFieldAccessExpression)?.identifier?.target != variable) return null
        return pair.first
    }
}