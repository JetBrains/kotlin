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

import org.jetbrains.kotlin.nj2k.types.JKJavaArrayType
import org.jetbrains.kotlin.nj2k.types.JKJavaPrimitiveType
import org.jetbrains.kotlin.nj2k.types.JKNoType
import kotlin.math.abs


class ForConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    private val referenceSearcher: ReferenceSearcher
        get() = context.converter.converterServices.oldServices.referenceSearcher

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaForLoopStatement) return recurse(element)

        convertToForeach(element)?.also { return recurse(it.withFormattingFrom(element)) }
        convertToWhile(element)?.also { return recurse(it.withFormattingFrom(element)) }

        return recurse(element)
    }

    private fun convertToWhile(loopStatement: JKJavaForLoopStatement): JKStatement? {
        val whileBody = createWhileBody(loopStatement)
        val condition =
            if (loopStatement.condition !is JKStubExpression) loopStatement::condition.detached()
            else JKLiteralExpression("true", JKLiteralExpression.LiteralType.BOOLEAN)
        val whileStatement = JKWhileStatement(condition, whileBody)

        if (loopStatement.initializers.isEmpty()
            || loopStatement.initializers.singleOrNull() is JKEmptyStatement
        ) return whileStatement

        val convertedFromForLoopSyntheticWhileStatement =
            JKKtConvertedFromForLoopSyntheticWhileStatement(
                loopStatement::initializers.detached(),
                whileStatement
            )

        val notNeedParentBlock = loopStatement.parent is JKBlock
                || loopStatement.parent is JKLabeledExpression && loopStatement.parent?.parent is JKBlock

        return when {
            loopStatement.hasNameConflict() ->
                JKExpressionStatement(
                    runExpression(
                        convertedFromForLoopSyntheticWhileStatement,
                        symbolProvider
                    )
                )
            !notNeedParentBlock -> blockStatement(convertedFromForLoopSyntheticWhileStatement)
            else -> convertedFromForLoopSyntheticWhileStatement
        }
    }

    private fun createWhileBody(loopStatement: JKJavaForLoopStatement): JKStatement {
        if (loopStatement.updaters.singleOrNull() is JKEmptyStatement) return loopStatement::body.detached()
        val continueStatementConverter = object : RecursiveApplicableConversionBase(context) {
            override fun applyToElement(element: JKTreeElement): JKTreeElement {
                if (element !is JKContinueStatement) return recurse(element)
                val elementPsi = element.psi<PsiContinueStatement>()!!
                if (elementPsi.findContinuedStatement()?.toContinuedLoop() != loopStatement.psi<PsiForStatement>()) return recurse(element)
                val statements = loopStatement.updaters.map { it.copyTreeAndDetach() } + element.copyTreeAndDetach()
                return if (element.parent is JKBlock)
                    JKBlockStatementWithoutBrackets(statements)
                else JKBlockStatement(JKBlockImpl(statements))
            }
        }

        val body = continueStatementConverter.applyToElement(loopStatement::body.detached())

        if (body is JKBlockStatement) {
            val hasNameConflict = loopStatement.initializers.any { initializer ->
                initializer is JKDeclarationStatement && initializer.declaredStatements.any { loopVar ->
                    loopVar is JKLocalVariable && body.statements.any { statement ->
                        statement is JKDeclarationStatement && statement.declaredStatements.any {
                            it is JKLocalVariable && it.name.value == loopVar.name.value
                        }
                    }
                }
            }

            val statements =
                if (hasNameConflict) {
                    listOf(JKExpressionStatement(runExpression(body, symbolProvider))) + loopStatement::updaters.detached()
                } else {
                    body.block::statements.detached() + loopStatement::updaters.detached()
                }
            return JKBlockStatement(JKBlockImpl(statements))
        } else {
            val statements =
                listOf(body as JKStatement) + loopStatement::updaters.detached()
            return JKBlockStatement(JKBlockImpl(statements))
        }
    }

    private fun convertToForeach(loopStatement: JKJavaForLoopStatement): JKForInStatement? {
        val initializer = loopStatement.initializers.singleOrNull() ?: return null
        val loopVar = (initializer as? JKDeclarationStatement)?.declaredStatements?.singleOrNull() as? JKLocalVariable ?: return null
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
            val range = forIterationRange(start, right, reversed, inclusive)
            val explicitType =
                if (context.converter.settings.specifyLocalVariableTypeByDefault)
                    JKJavaPrimitiveType.INT
                else JKNoType
            val loopVarDeclaration =
                JKForLoopVariable(
                    JKTypeElement(explicitType),
                    loopVar::name.detached(),
                    JKStubExpression()
                )
            return JKForInStatement(
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
        inclusiveComparison: Boolean
    ): JKExpression {
        indicesIterationRange(start, bound, reversed, inclusiveComparison)?.also { return it }
        return when {
            reversed -> downToExpression(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else +1),
                context
            )
            bound !is JKLiteralExpression && !inclusiveComparison ->
                untilToExpression(
                    start,
                    convertBound(bound, 0),
                    context
                )
            else -> JKBinaryExpression(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else -1),
                JKKtOperatorImpl(
                    JKOperatorToken.RANGE,
                    typeFactory.types.nullableAny //todo range type
                )
            )
        }
    }

    private fun convertBound(bound: JKExpression, correction: Int): JKExpression {
        if (correction == 0) return bound

        if (bound is JKLiteralExpression && bound.type == JKLiteralExpression.LiteralType.INT) {
            val value = bound.literal.toInt()
            return JKLiteralExpression((value + correction).toString(), bound.type)
        }

        val sign = if (correction > 0) JKOperatorToken.PLUS else JKOperatorToken.MINUS
        return JKBinaryExpression(
            bound,
            JKLiteralExpression(abs(correction).toString(), JKLiteralExpression.LiteralType.INT),
            JKKtOperatorImpl(
                sign,
                typeFactory.types.int
            )
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
            JKQualifiedExpression(
                indices,
                JKCallExpressionImpl(
                    symbolProvider.provideMethodSymbol("kotlin.collections.reversed"),
                    JKArgumentList()
                )
            )
        } else indices
    }


    private fun indicesByCollectionSize(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKCallExpression ?: return null
        return if (methodCall.identifier.deepestFqName() == "java.util.Collection.size"
            && methodCall.arguments.arguments.isEmpty()
        ) toIndicesCall(javaSizeCall) else null
    }

    private fun indicesByArrayLength(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKFieldAccessExpression ?: return null
        val receiverType = javaSizeCall.receiver.calculateType(typeFactory)
        if (methodCall.identifier.name == "length" && receiverType is JKJavaArrayType) {
            return toIndicesCall(javaSizeCall)
        }
        return null
    }

    private fun toIndicesCall(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        if (javaSizeCall.psi == null) return null
        val selector = JKFieldAccessExpression(
            symbolProvider.provideFieldSymbol("kotlin.collections.indices")
        )
        return JKQualifiedExpression(javaSizeCall::receiver.detached(), selector)
    }

    private fun JKJavaForLoopStatement.hasNameConflict(): Boolean {
        val names = initializers.flatMap { it.declaredVariableNames() }
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
            is JKJavaForLoopStatement -> initializers.flatMap { it.declaredVariableNames() }
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