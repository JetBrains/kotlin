/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.ast.Mutability
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
        val condition =
            if (loopStatement.condition !is JKStubExpression) loopStatement::condition.detached()
            else JKKtLiteralExpressionImpl("true", JKLiteralExpression.LiteralType.BOOLEAN)
        val whileStatement = JKWhileStatementImpl(condition, whileBody)

        if (loopStatement.initializer is JKEmptyStatement) return whileStatement
        //TODO check for error conflict

        return JKKtConvertedFromForLoopSyntheticWhileStatementImpl(
            loopStatement::initializer.detached(),
            JKWhileStatementImpl(condition, whileBody)
        )
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
                    JKBlockStatementWithoutBracketsImpl(JKBlockImpl(statements))
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
                            it is JKLocalVariable && it.name == loopVar.name
                        }
                    }
                }

            val statements =
                if (hasNameConflict) {

                    listOf(body) + loopStatement::updaters.detached()
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
            //TODO for loop explicitType
//            val explicitType = if (context.converter.settings.specifyLocalVariableTypeByDefault)
//                JKJavaPrimitiveTypeImpl.INT
//            else null
            val loopVarDeclaration =
                JKForLoopVariableImpl(
                    JKTypeElementImpl(JKNoTypeImpl),
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
            else -> kotlinBinaryExpression(
                start,
                convertBound(bound, if (inclusiveComparison) 0 else -1),
                JKKtSingleValueOperatorToken(KtTokens.RANGE),
                context
            )!!
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
            JKKtLiteralExpressionImpl(Math.abs(correction).toString(), JKLiteralExpression.LiteralType.INT),
            JKKtSingleValueOperatorToken(sign),
            context
        )!!
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

        val psiContext = collectionSizeExpression.psi<PsiExpression>() ?: return null
        return if (reversed) {
            val reversedSymbol = context.symbolProvider.provideDirectSymbol(
                multiResolveFqName(ClassId.fromString("kotlin/collections/reversed"), psiContext).first()
            ) as JKMethodSymbol
            JKQualifiedExpressionImpl(
                indices,
                JKKtQualifierImpl.DOT,
                JKJavaMethodCallExpressionImpl(reversedSymbol, JKExpressionListImpl())
            )
        } else indices
    }


    private fun indicesByCollectionSize(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKMethodCallExpression ?: return null
        val receiverType = javaSizeCall.receiver.type(context) as? JKClassType ?: return null

        //TODO check if receiver type is Collection
        if (methodCall.identifier.name == "size" && methodCall.arguments.expressions.isEmpty()) {
            return toIndicesCall(javaSizeCall)
        }
        return null
    }

    private fun indicesByArrayLength(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val methodCall = javaSizeCall.selector as? JKFieldAccessExpression ?: return null
        val receiverType = javaSizeCall.receiver.type(context)
        if (methodCall.identifier.name == "length" && receiverType is JKJavaArrayType) {
            return toIndicesCall(javaSizeCall)
        }
        return null
    }

    private fun toIndicesCall(javaSizeCall: JKQualifiedExpression): JKQualifiedExpression? {
        val psiContext = javaSizeCall.psi ?: return null
        val indiciesSymbol = context.symbolProvider.provideDirectSymbol(
            multiResolveFqName(ClassId.fromString("kotlin/collections/indices"), psiContext).first()
        ) as JKMultiversePropertySymbol
        val selector = JKFieldAccessExpressionImpl(indiciesSymbol)
        return JKQualifiedExpressionImpl(javaSizeCall::receiver.detached(), javaSizeCall.operator, selector)
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