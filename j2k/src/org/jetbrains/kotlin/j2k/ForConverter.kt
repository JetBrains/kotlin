/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings

class ForConverter(
        private val statement: PsiForStatement,
        private val codeConverter: CodeConverter
) {
    private val referenceSearcher = codeConverter.converter.referenceSearcher
    private val settings = codeConverter.settings
    private val project = codeConverter.converter.project

    private val initialization = statement.initialization
    private val update = statement.update
    private val condition = statement.condition
    private val body = statement.body

    fun execute(): Statement {
        val foreach = convertToForeach()
        if (foreach != null) return foreach

        val initialization = statement.initialization
        val update = statement.update
        val condition = statement.condition
        val body = statement.body

        val initializationConverted = codeConverter.convertStatement(initialization)
        val updateConverted = codeConverter.convertStatement(update)

        val whileBody = if (updateConverted.isEmpty) {
            codeConverter.convertStatementOrBlock(body)
        }
        else {
            // we should process all continue-statements because we need to add update statement(s) before them
            val codeConverterToUse = codeConverter.withSpecialStatementConverter(object : SpecialStatementConverter {
                override fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement? {
                    if (statement !is PsiContinueStatement) return null
                    if (statement.findContinuedStatement()?.toContinuedLoop() != this@ForConverter.statement) return null

                    val continueConverted = this@ForConverter.codeConverter.convertStatement(statement)
                    val statements = listOf(updateConverted, continueConverted)
                    if (statement.parent is PsiCodeBlock) {
                        // generate fictive statement which will generate multiple statements
                        return object : Statement() {
                            override fun generateCode(builder: CodeBuilder) {
                                builder.append(statements, "\n")
                            }
                        }
                    }
                    else {
                        return Block.of(statements)
                    }
                }
            })

            if (body is PsiBlockStatement) {
                val nameConflict = initialization is PsiDeclarationStatement && initialization.declaredElements.any { loopVar ->
                    loopVar is PsiNamedElement && body.codeBlock.statements.any { statement ->
                        statement is PsiDeclarationStatement && statement.declaredElements.any {
                            it is PsiNamedElement && it.name == loopVar.name
                        }
                    }
                }

                if (nameConflict) {
                    val statements = listOf(codeConverterToUse.convertStatement(body), updateConverted)
                    Block.of(statements).assignNoPrototype()
                }
                else {
                    val block = codeConverterToUse.convertBlock(body.codeBlock, true)
                    Block(block.statements + listOf(updateConverted), block.lBrace, block.rBrace, true).assignPrototypesFrom(block)
                }
            }
            else {
                val statements = listOf(codeConverterToUse.convertStatement(body), updateConverted)
                Block.of(statements).assignNoPrototype()
            }
        }

        val whileStatement = WhileStatement(
                if (condition != null) codeConverter.convertExpression(condition) else LiteralExpression("true").assignNoPrototype(),
                whileBody,
                statement.isInSingleLine()).assignNoPrototype()
        if (initializationConverted.isEmpty) return whileStatement

        val kind = if (statement.parents.filter { it !is PsiLabeledStatement }.first() !is PsiCodeBlock) {
            WhileWithInitializationPseudoStatement.Kind.WITH_BLOCK
        }
        else if (hasNameConflict())
            WhileWithInitializationPseudoStatement.Kind.WITH_RUN_BLOCK
        else
            WhileWithInitializationPseudoStatement.Kind.SIMPLE
        return WhileWithInitializationPseudoStatement(initializationConverted, whileStatement, kind)
    }

    class WhileWithInitializationPseudoStatement(
            val initialization: Statement,
            val loop: Statement,
            val kind: WhileWithInitializationPseudoStatement.Kind
    ) : Statement() {

        enum class Kind {
            SIMPLE,
            WITH_BLOCK,
            WITH_RUN_BLOCK
        }

        private val statements = listOf(initialization, loop)

        override fun generateCode(builder: CodeBuilder) {
            if (kind == Kind.SIMPLE) {
                builder.append(statements, "\n")
            }
            else {
                val block = Block.of(statements).assignNoPrototype()
                if (kind == Kind.WITH_BLOCK) {
                    block.generateCode(builder)
                }
                else {
                    val argumentList = ArgumentList.withNoPrototype(LambdaExpression(null, block))
                    val call = MethodCallExpression.buildNonNull(null, "run", argumentList)
                    call.generateCode(builder)
                }
            }
        }
    }

    private fun convertToForeach(): ForeachStatement? {
        if (initialization is PsiDeclarationStatement) {
            val loopVar = initialization.declaredElements.singleOrNull() as? PsiLocalVariable ?: return null
            if (!loopVar.hasWriteAccesses(referenceSearcher, body)
                && !loopVar.hasWriteAccesses(referenceSearcher, condition)
                && condition is PsiBinaryExpression) {

                val left = condition.lOperand as? PsiReferenceExpression ?: return null
                val right = condition.rOperand ?: return null
                if (right.type == PsiType.DOUBLE || right.type == PsiType.FLOAT || right.type == PsiType.CHAR) {
                    return null
                }

                if (left.resolve() == loopVar) {
                    val start = loopVar.initializer ?: return null
                    val operationType = (update as? PsiExpressionStatement)?.expression?.isVariableIncrementOrDecrement(loopVar)
                    val reversed = when (operationType) {
                        JavaTokenType.PLUSPLUS -> false
                        JavaTokenType.MINUSMINUS -> true
                        else -> return null
                    }

                    val inclusive = when (condition.operationTokenType) {
                        JavaTokenType.LT -> if (reversed) return null else false
                        JavaTokenType.LE -> if (reversed) return null else true
                        JavaTokenType.GT -> if (reversed) false else return null
                        JavaTokenType.GE -> if (reversed) true else return null
                        JavaTokenType.NE -> false
                        else -> return null
                    }

                    val range = forIterationRange(start, right, reversed, inclusive).assignNoPrototype()
                    val explicitType = if (settings.specifyLocalVariableTypeByDefault)
                        PrimitiveType(Identifier.withNoPrototype("Int")).assignNoPrototype()
                    else
                        null
                    return ForeachStatement(loopVar.declarationIdentifier(), explicitType, range, codeConverter.convertStatementOrBlock(body), statement.isInSingleLine())
                }
            }
        }
        return null
    }

    private fun PsiElement.isVariableIncrementOrDecrement(variable: PsiVariable): IElementType? {
        //TODO: simplify code when KT-5453 fixed
        val pair = when (this) {
            is PsiPostfixExpression -> operationTokenType to operand
            is PsiPrefixExpression -> operationTokenType to operand
            else -> return null
        }
        if ((pair.second as? PsiReferenceExpression)?.resolve() != variable) return null
        return pair.first
    }

    private fun forIterationRange(start: PsiExpression, bound: PsiExpression, reversed: Boolean, inclusiveComparison: Boolean): Expression {
        val indicesRange = indicesIterationRange(start, bound, reversed, inclusiveComparison)
        if (indicesRange != null) return indicesRange

        val startConverted = codeConverter.convertExpression(start)
        return if (reversed)
            DownToExpression(startConverted, convertBound(bound, if (inclusiveComparison) 0 else +1))
        else
            RangeExpression(startConverted, convertBound(bound, if (inclusiveComparison) 0 else -1))
    }

    private fun indicesIterationRange(start: PsiExpression, bound: PsiExpression, reversed: Boolean, inclusiveComparison: Boolean): Expression? {
        val collectionSize = if (reversed) {
            if (!inclusiveComparison) return null

            if ((bound as? PsiLiteralExpression)?.value != 0) return null

            if (start !is PsiBinaryExpression) return null
            if (start.operationTokenType != JavaTokenType.MINUS) return null
            if ((start.rOperand as? PsiLiteralExpression)?.value != 1) return null
            start.lOperand
        }
        else {
            if (inclusiveComparison) return null
            if ((start as? PsiLiteralExpression)?.value != 0) return null
            bound
        }


        var indices: Expression? = null

        // check if it's iteration through list indices
        if (collectionSize is PsiMethodCallExpression && collectionSize.argumentList.expressions.isEmpty()) {
            val methodExpr = collectionSize.methodExpression
            if (methodExpr is PsiReferenceExpression && methodExpr.referenceName == "size") {
                val qualifier = methodExpr.qualifierExpression
                if (qualifier is PsiReferenceExpression /* we don't convert to .indices if qualifier is method call or something because of possible side effects */) {
                    val collectionType = PsiElementFactory.SERVICE.getInstance(project).createTypeByFQClassName(CommonClassNames.JAVA_UTIL_COLLECTION)
                    val qualifierType = qualifier.type
                    if (qualifierType != null && collectionType.isAssignableFrom(qualifierType)) {
                        indices = QualifiedExpression(codeConverter.convertExpression(qualifier), Identifier.withNoPrototype("indices", isNullable = false), null)
                    }
                }
            }
        }
        // check if it's iteration through array indices
        else if (collectionSize is PsiReferenceExpression /* we don't convert to .indices if qualifier is method call or something because of possible side effects */
            && collectionSize.referenceName == "length") {
            val qualifier = collectionSize.qualifierExpression
            if (qualifier is PsiReferenceExpression && qualifier.type is PsiArrayType) {
                indices = QualifiedExpression(codeConverter.convertExpression(qualifier), Identifier.withNoPrototype("indices", isNullable = false), null)
            }
        }

        if (indices == null) return null

        return if (reversed)
            MethodCallExpression.buildNonNull(indices.assignNoPrototype(), "reversed")
        else
            indices
    }

    private fun convertBound(bound: PsiExpression, correction: Int): Expression {
        if (correction == 0) {
            return codeConverter.convertExpression(bound)
        }

        if (bound is PsiLiteralExpression) {
            val value = bound.value
            if (value is Int) {
                return LiteralExpression((value + correction).toString()).assignPrototype(bound)
            }
        }

        val converted = codeConverter.convertExpression(bound)
        val sign = if (correction > 0) JavaTokenType.PLUS else JavaTokenType.MINUS
        return BinaryExpression(converted, LiteralExpression(Math.abs(correction).toString()).assignNoPrototype(), Operator(sign).assignPrototype(bound)).assignNoPrototype()
    }

    private fun PsiStatement.toContinuedLoop(): PsiLoopStatement? {
        return when (this) {
            is PsiLoopStatement -> this
            is PsiLabeledStatement -> this.statement?.toContinuedLoop()
            else -> null
        }
    }

    private fun hasNameConflict(): Boolean {
        val names = statement.initialization?.declaredVariableNames() ?: return false
        if (names.isEmpty()) return false

        val factory = PsiElementFactory.SERVICE.getInstance(project)
        for (name in names) {
            val refExpr = try {
                factory.createExpressionFromText(name, statement) as? PsiReferenceExpression ?: return true
            }
            catch(e: IncorrectOperationException) {
                return true
            }
            if (refExpr.resolve() != null) return true
        }

        var hasConflict = false
        for (laterStatement in statement.siblings(forward = true, withItself = false)) {
            laterStatement.accept(object: JavaRecursiveElementVisitor() {
                override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
                    super.visitDeclarationStatement(statement)

                    if (statement.declaredVariableNames().any { it in names }) {
                        hasConflict = true
                    }
                }
            })
        }

        return hasConflict
    }

    private fun PsiStatement.declaredVariableNames(): Collection<String> {
        val declarationStatement = this as? PsiDeclarationStatement ?: return listOf()
        return declarationStatement.declaredElements
                .filterIsInstance<PsiVariable>()
                .map { it.name!! }
    }
}
