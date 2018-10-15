/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ReferenceSearcher
import org.jetbrains.kotlin.j2k.hasWriteAccesses
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId


class ForConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    private val referenceSearcher: ReferenceSearcher
        get() = context.converter.converterServices.oldServices.referenceSearcher

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaForLoopStatement) return recurse(element)

        convertToForeach(element)?.also { return recurse(it) }
        convertToWhile(element)?.also { return recurse(it) }

        return recurse(element)
    }

    private fun convertToWhile(loopStatement: JKJavaForLoopStatement): JKStatement? {
        val whileBody = createWhileBody(loopStatement)

        val whileStatement =
            JKWhileStatementImpl(
                if (loopStatement.condition !is JKStubExpression) loopStatement.condition.detached()
                else JKKtLiteralExpressionImpl("true", JKLiteralExpression.LiteralType.BOOLEAN),
                whileBody
            )
        val whileStatement = JKWhileStatementImpl(condition, whileBody)

        if (loopStatement.initializer is JKEmptyStatement) return whileStatement
        //TODO check for error conflict
        return JKBlockStatementImpl(JKBlockImpl(listOf(loopStatement.initializer.detached(), whileStatement)))
    }

    private fun createWhileBody(loopStatement: JKJavaForLoopStatement): JKStatement {
        if (loopStatement.updater is JKEmptyStatement) return loopStatement::body.detached()
        val continueStatementConverter = object : RecursiveApplicableConversionBase() {
            override fun applyToElement(element: JKTreeElement): JKTreeElement {
                if (element !is JKContinueStatement) return recurse(element)
                val elementPsi = element.psi<PsiContinueStatement>()!!
                if (elementPsi.findContinuedStatement()?.toContinuedLoop() != loopStatement.psi<PsiForStatement>()) return recurse(element)
                val statements = listOf(loopStatement.updater, element)
                return recurse(JKBlockStatementImpl(JKBlockImpl(statements)))
            }

        }

        val body = loopStatement.body

        if (body is JKBlockStatement) {
            val initializer = loopStatement.initializer
            val hasNameConflict =
                initializer is JKDeclarationStatement && initializer.declaredStatements.any { loopVar ->
                    loopVar is JKLocalVariable && body.statements.any { statement ->
                        statement is JKDeclarationStatement && statement.declaredStatements.any {
                            it is JKLocalVariable && it.name == loopVar.name
                        }
                    }
                }

            val statements =
                if (hasNameConflict) {
                    listOf(continueStatementConverter.applyToElement(body) as JKStatement, loopStatement.updater)
                } else {
                    body.block.statements + loopStatement.updater
                }
            return JKBlockStatementImpl(JKBlockImpl(statements.map { it.detached() }))
        } else {
            val statements =
                listOf(continueStatementConverter.applyToElement(body) as JKStatement, loopStatement::updater.detached())
            return JKBlockStatementImpl(JKBlockImpl(statements.map { it.detached() }))
        }
    }

    private fun convertToForeach(loopStatement: JKJavaForLoopStatement): JKKtForInStatement? {
        val loopVar =
            (loopStatement.initializer as? JKDeclarationStatement)?.declaredStatements?.singleOrNull() as? JKLocalVariable ?: return null
        val loopVarPsi = loopVar.psi<PsiLocalVariable>() ?: return null
        val condition = loopStatement.condition as? JKBinaryExpression ?: return null
        if (!loopVarPsi.hasWriteAccesses(referenceSearcher, loopStatement.body.psi())
            && !loopVarPsi.hasWriteAccesses(referenceSearcher, loopStatement.condition.psi())
        ) {
            val left = condition.left as? JKFieldAccessExpression ?: return null
            val right = condition.right
            if (right.psi<PsiExpression>()?.type in listOf(PsiType.DOUBLE, PsiType.FLOAT, PsiType.CHAR)) return null
            if (left.identifier.target != loopVar) return null
            val start = loopVar.initializer
            val operationType =
                (loopStatement.updater as? JKExpressionStatement)?.expression?.isVariableIncrementOrDecrement(loopVar)
            val reversed = when ((operationType  as? JKJavaOperatorImpl)?.token) {
                JavaTokenType.PLUSPLUS -> false
                JavaTokenType.MINUSMINUS -> true
                else -> return null
            }
            val inclusive = when ((condition.operator as? JKJavaOperatorImpl)?.token ?: (condition.operator as? JKKtOperatorImpl)?.token) {
                JavaTokenType.LT, KtTokens.LT -> if (reversed) return null else false
                JavaTokenType.LE, KtTokens.LTEQ -> if (reversed) return null else true
                JavaTokenType.GT, KtTokens.GT -> if (reversed) false else return null
                JavaTokenType.GE, KtTokens.GTEQ -> if (reversed) true else return null
                JavaTokenType.NE, KtTokens.EXCLEQ -> false
                else -> return null
            }
            val range = forIterationRange(start, right, reversed, inclusive, loopVarPsi)
            //TODO for loop explicitType
//            val explicitType = if (context.converter.settings.specifyLocalVariableTypeByDefault)
//                JKJavaPrimitiveTypeImpl.INT
//            else null
            return JKKtForInStatementImpl(
                loopVar.name.detached(),
                range.detached(),
                loopStatement.body.detached()
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
        if (start.parent != null) start.detach(start.parent!!)
        if (bound.parent != null) bound.detach(bound.parent!!)
        return when {
            reversed -> downToExpression(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else +1),
                context,
                psiContext
            )
            bound !is JKKtLiteralExpression && !inclusiveComparison ->
                untilToExpression(
                    start,
                    convertBound(bound, 0),
                    context,
                    psiContext
                )
            else -> JKBinaryExpressionImpl(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else -1),
                JKKtOperatorImpl.tokenToOperator[KtTokens.RANGE]!!
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
        return JKBinaryExpressionImpl(
            bound,
            JKKtLiteralExpressionImpl(Math.abs(correction).toString(), JKLiteralExpression.LiteralType.INT),
            JKKtOperatorImpl.tokenToOperator[sign]!!
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
                if ((start.operator as? JKKtOperatorImpl)?.token != KtTokens.MINUS) return null
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

        val psiContext = collectionSizeExpression.psi<PsiExpression>() ?: return null
        return if (reversed) {
            val reversedSymbol = context.symbolProvider.provideDirectSymbol(
                multiResolveFqName(ClassId.fromString("kotlin/collections/reversed"), psiContext).first()
            ) as JKMethodSymbol
            JKQualifiedExpressionImpl(
                indices.detached(),
                JKKtQualifierImpl.DOT,
                JKJavaMethodCallExpressionImpl(reversedSymbol, JKExpressionListImpl())
            )
        } else indices
    }


    private fun indicesByCollectionSize(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKMethodCallExpression ?: return null
        //TODO check if receiver type is Collection
        if (methodCall.identifier.name == "size" && methodCall.arguments.expressions.isEmpty()) {
            return toIndicesCall(javaSizeCall)
        }
        return null
    }

    private fun indicesByArrayLength(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKFieldAccessExpression ?: return null
        val receiverType = javaSizeCall.receiver.type(context)
        //TODO check if receiver type is kotlin.array
        if (methodCall.identifier.name == "length" && receiverType is JKJavaArrayType) {
            return toIndicesCall(javaSizeCall)
        }
        return null
    }

    private fun toIndicesCall(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val psiContext = javaSizeCall.psi<PsiExpression>() ?: return null
        val indiciesSymbol = context.symbolProvider.provideDirectSymbol(
            multiResolveFqName(ClassId.fromString("kotlin/collections/indices"), psiContext).first()
        ) as JKMultiversePropertySymbol
        javaSizeCall.selector = JKFieldAccessExpressionImpl(indiciesSymbol)
        return javaSizeCall
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

    private inline fun <reified ElementType : PsiElement> JKElement.psi() =
        context.backAnnotator(this) as? ElementType
}