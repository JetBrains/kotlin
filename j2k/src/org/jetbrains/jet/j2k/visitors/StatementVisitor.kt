/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.countWriteAccesses
import java.util.ArrayList

class StatementVisitor(public val converter: Converter) : JavaElementVisitor() {
    public var result: Statement = Statement.Empty
        private set

    override fun visitAssertStatement(statement: PsiAssertStatement) {
        result = AssertStatement(converter.convertExpression(statement.getAssertCondition()),
                                   converter.convertExpression(statement.getAssertDescription()))
    }

    override fun visitBlockStatement(statement: PsiBlockStatement) {
        result = converter.convertBlock(statement.getCodeBlock(), true)
    }

    override fun visitBreakStatement(statement: PsiBreakStatement) {
        if (statement.getLabelIdentifier() == null) {
            result = BreakStatement(Identifier.Empty)
        }
        else {
            result = BreakStatement(converter.convertIdentifier(statement.getLabelIdentifier()))
        }
    }

    override fun visitContinueStatement(statement: PsiContinueStatement) {
        if (statement.getLabelIdentifier() == null) {
            result = ContinueStatement(Identifier.Empty)
        }
        else {
            result = ContinueStatement(converter.convertIdentifier(statement.getLabelIdentifier()))
        }
    }

    override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
        result = DeclarationStatement(converter.convertElements(statement.getDeclaredElements()))
    }

    override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
        val condition = statement.getCondition()
        val expression = if (condition != null && condition.getType() != null)
            converter.convertExpression(condition, condition.getType())
        else
            converter.convertExpression(condition)
        result = DoWhileStatement(expression, converter.convertStatement(statement.getBody()))
    }

    override fun visitExpressionStatement(statement: PsiExpressionStatement) {
        result = converter.convertExpression(statement.getExpression())
    }

    override fun visitExpressionListStatement(statement: PsiExpressionListStatement) {
        result = ExpressionListStatement(converter.convertExpressions(statement.getExpressionList().getExpressions()))
    }

    override fun visitForStatement(statement: PsiForStatement) {
        val initialization = statement.getInitialization()
        val update = statement.getUpdate()
        val condition = statement.getCondition()
        val body = statement.getBody()
        val firstChildLocalVar = initialization?.getFirstChild() as? PsiLocalVariable
        var bodyWriteCount = countWriteAccesses(firstChildLocalVar, body)
        var conditionWriteCount = countWriteAccesses(firstChildLocalVar, condition)
        var updateWriteCount = countWriteAccesses(firstChildLocalVar, update)
        val onceWritableIterator = updateWriteCount == 1 && bodyWriteCount + conditionWriteCount == 0
        val operationTokenType = (condition as? PsiBinaryExpression)?.getOperationTokenType()
        if (initialization is PsiDeclarationStatement
                && initialization.getFirstChild() == initialization.getLastChild()
                && condition != null
                && update != null
                && update.getChildren().size == 1
                && isPlusPlusExpression(update.getChildren().single())
                && (operationTokenType == JavaTokenType.LT || operationTokenType == JavaTokenType.LE)
                && firstChildLocalVar != null
                && firstChildLocalVar.getNameIdentifier() != null
                && onceWritableIterator) {
            val end = converter.convertExpression((condition as PsiBinaryExpression).getROperand())
            val endExpression = if (operationTokenType == JavaTokenType.LT)
                BinaryExpression(end, Identifier("1"), "-")
            else
                end
            result = ForeachWithRangeStatement(Identifier(firstChildLocalVar.getName()!!),
                                                 converter.convertExpression(firstChildLocalVar.getInitializer()),
                                                 endExpression,
                                                 converter.convertStatement(body))
        }
        else {
            var forStatements = ArrayList<Statement>()
            forStatements.add(converter.convertStatement(initialization))
            val bodyAndUpdate = listOf(converter.convertStatement(body),
                                       Block(listOf(converter.convertStatement(update))))
            forStatements.add(WhileStatement(
                    if (condition == null)
                        LiteralExpression("true")
                    else
                        converter.convertExpression(condition),
                    Block(bodyAndUpdate)))
            result = Block(forStatements)
        }
    }

    override fun visitForeachStatement(statement: PsiForeachStatement) {
        val iterator = run {
            val iteratorExpr = converter.convertExpression(statement.getIteratedValue())
            if (iteratorExpr.isNullable)
                BangBangExpression(iteratorExpr)
            else
                iteratorExpr
        }
        result = ForeachStatement(converter.convertParameter(statement.getIterationParameter()),
                                  iterator,
                                  converter.convertStatement(statement.getBody()))
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        val condition = statement.getCondition()
        val expression = converter.convertExpression(condition, PsiType.BOOLEAN)
        result = IfStatement(expression,
                               converter.convertStatement(statement.getThenBranch()),
                               converter.convertStatement(statement.getElseBranch()))
    }

    override fun visitLabeledStatement(statement: PsiLabeledStatement) {
        result = LabelStatement(converter.convertIdentifier(statement.getLabelIdentifier()),
                                converter.convertStatement(statement.getStatement()))
    }

    override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement) {
        result = if (statement.isDefaultCase())
            DefaultSwitchLabelStatement()
        else
            SwitchLabelStatement(converter.convertExpression(statement.getCaseValue()))
    }

    override fun visitSwitchStatement(statement: PsiSwitchStatement) {
        result = SwitchContainer(converter.convertExpression(statement.getExpression()),
                                   switchBodyToCases(statement.getBody()))
    }

    private fun switchBodyToCases(body: PsiCodeBlock?): List<CaseContainer> {
        val cases: List<List<PsiElement>> = splitToCases(body)
        val allSwitchStatements = ArrayList<PsiElement>()
        if (body != null) {
            allSwitchStatements.addAll(body.getStatements())
        }
        val result = ArrayList<CaseContainer>()
        var pendingLabels = ArrayList<Element>()
        var i = 0
        var hasDefaultCase: Boolean = false
        for (ls in cases) {
            // TODO assert {(ls?.size()).sure() > 0}
            if (ls.size() > 0) {
                var label = ls[0] as PsiSwitchLabelStatement
                hasDefaultCase = hasDefaultCase || label.isDefaultCase()
                // TODO assert {(label is PsiSwitchLabelStatement?)}
                // TODO assert("not a right index") {allSwitchStatements?.get(i) == label}
                if (ls.size() > 1) {
                    pendingLabels.add(converter.convertStatement(label))
                    val slice: List<PsiElement> = ls.subList(1, (ls.size()))
                    if (!containsBreak(slice)) {
                        val statements = ArrayList(converter.convertStatements(slice).statements)
                        statements.addAll(converter.convertStatements(getAllToNextBreak(allSwitchStatements, i + ls.size())).statements)
                        result.add(CaseContainer(pendingLabels, statements))
                        pendingLabels = ArrayList()
                    }
                    else {
                        result.add(CaseContainer(pendingLabels, converter.convertStatements(slice).statements))
                        pendingLabels = ArrayList()
                    }
                }
                else {
                    pendingLabels.add(converter.convertStatement(label))
                }
                i += ls.size()
            }
        }
        if (!hasDefaultCase)
            result.add(CaseContainer(listOf(DefaultSwitchLabelStatement()), ArrayList()))
        return result
    }

    override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement) {
        result = SynchronizedStatement(converter.convertExpression(statement.getLockExpression()),
                                         converter.convertBlock(statement.getBody()))
    }

    override fun visitThrowStatement(statement: PsiThrowStatement) {
        result = ThrowStatement(converter.convertExpression(statement.getException()))
    }

    override fun visitTryStatement(statement: PsiTryStatement) {
        val catches = ArrayList<CatchStatement>()
        val catchBlocks = statement.getCatchBlocks()
        val catchBlockParameters = statement.getCatchBlockParameters()
        for (i in 0..catchBlocks.size - 1) {
            catches.add(CatchStatement(converter.convertParameter(catchBlockParameters[i], true),
                                       converter.convertBlock(catchBlocks[i], true)))
        }
        result = TryStatement(converter.convertBlock(statement.getTryBlock(), true),
                                catches, converter.convertBlock(statement.getFinallyBlock(), true))
    }

    override fun visitWhileStatement(statement: PsiWhileStatement) {
        var condition: PsiExpression? = statement.getCondition()
        val expression: Expression = (if (condition != null && condition?.getType() != null)
            this.converter.convertExpression(condition, condition?.getType())
        else
            converter.convertExpression(condition))
        result = WhileStatement(expression, converter.convertStatement(statement.getBody()))
    }

    override fun visitReturnStatement(statement: PsiReturnStatement) {
        val returnValue = statement.getReturnValue()
        val methodReturnType = converter.methodReturnType
        val expression = (if (returnValue != null && methodReturnType != null)
            this.converter.convertExpression(returnValue, methodReturnType)
        else
            converter.convertExpression(returnValue))
        result = ReturnStatement(expression)
    }

    override fun visitEmptyStatement(statement: PsiEmptyStatement) {
        result = Statement.Empty
    }

    private fun isPlusPlusExpression(psiElement: PsiElement): Boolean {
        return (psiElement is PsiPostfixExpression && psiElement.getOperationTokenType() == JavaTokenType.PLUSPLUS) ||
                (psiElement is PsiPrefixExpression && psiElement.getOperationTokenType() == JavaTokenType.PLUSPLUS)
    }

    private fun containsBreak(slice: List<PsiElement?>) = slice.any { it is PsiBreakStatement }

    private fun getAllToNextBreak(allStatements: List<PsiElement>, start: Int): List<PsiElement> {
        val result = ArrayList<PsiElement>()
        for (i in start..allStatements.size() - 1) {
            val s = allStatements[i]
            if (s is PsiBreakStatement || s is PsiReturnStatement) {
                return result
            }

            if (s !is PsiSwitchLabelStatement) {
                result.add(s)
            }

        }
        return result
    }

    private fun splitToCases(body: PsiCodeBlock?): List<List<PsiElement>> {
        val cases = ArrayList<List<PsiElement>>()
        var currentCaseStatements = ArrayList<PsiElement>()
        if (body != null) {
            var isFirst = true
            for (s in body.getChildren()) {
                if (s !is PsiStatement && s !is PsiComment) continue
                if (s is PsiSwitchLabelStatement) {
                    if (isFirst) {
                        isFirst = false
                    }
                    else {
                        cases.add(currentCaseStatements)
                        currentCaseStatements = ArrayList()
                    }
                }

                currentCaseStatements.add(s)
            }
            cases.add(currentCaseStatements)
        }

        return cases
    }
}
