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
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.countWritingAccesses
import java.util.ArrayList

public open class StatementVisitor(converter: Converter) : ElementVisitor(converter) {
    public override fun visitAssertStatement(statement: PsiAssertStatement?) {
        myResult = AssertStatement(getConverter().convertExpression(statement?.getAssertCondition()),
                                   getConverter().convertExpression(statement?.getAssertDescription()))
    }

    public override fun visitBlockStatement(statement: PsiBlockStatement?) {
        myResult = getConverter().convertBlock(statement?.getCodeBlock(), true)
    }

    public override fun visitBreakStatement(statement: PsiBreakStatement?) {
        if (statement?.getLabelIdentifier() == null) {
            myResult = BreakStatement(Identifier.EMPTY_IDENTIFIER)
        }
        else
        {
            myResult = BreakStatement(getConverter().convertIdentifier(statement?.getLabelIdentifier()))
        }
    }

    public override fun visitContinueStatement(statement: PsiContinueStatement?) {
        if (statement?.getLabelIdentifier() == null)
        {
            myResult = ContinueStatement(Identifier.EMPTY_IDENTIFIER)
        }
        else
        {
            myResult = ContinueStatement(getConverter().convertIdentifier(statement?.getLabelIdentifier()))
        }
    }

    public override fun visitDeclarationStatement(statement: PsiDeclarationStatement?) {
        myResult = DeclarationStatement(getConverter().convertElements(statement?.getDeclaredElements()!!))
    }

    public override fun visitDoWhileStatement(statement: PsiDoWhileStatement?) {
        val condition: PsiExpression? = statement?.getCondition()
        val expression: Expression = (if (condition != null && condition.getType() != null)
            getConverter().convertExpression(condition, condition.getType())
        else
            getConverter().convertExpression(condition))
        myResult = DoWhileStatement(expression, getConverter().convertStatement(statement?.getBody()))
    }

    public override fun visitExpressionStatement(statement: PsiExpressionStatement?) {
        myResult = getConverter().convertExpression(statement?.getExpression())
    }

    public override fun visitExpressionListStatement(statement: PsiExpressionListStatement?) {
        myResult = ExpressionListStatement(getConverter().convertExpressions(
                statement?.getExpressionList()?.getExpressions()!!))
    }

    public override fun visitForStatement(statement: PsiForStatement?) {
        val initialization = statement?.getInitialization()
        val update = statement?.getUpdate()
        val condition = statement?.getCondition()
        val body = statement?.getBody()
        val firstChild = (if (initialization != null && (initialization.getFirstChild() is PsiLocalVariable))
            (initialization.getFirstChild() as PsiLocalVariable)
        else
            null)
        var bodyWriteCount = countWritingAccesses(firstChild, body)
        var conditionWriteCount = countWritingAccesses(firstChild, condition)
        var updateWriteCount = countWritingAccesses(firstChild, update)
        val onceWritableIterator = updateWriteCount == 1 && bodyWriteCount + conditionWriteCount == 0
        val operationTokenType = (if (condition is PsiBinaryExpression)
            condition.getOperationTokenType()
        else
            null)
        if (initialization is PsiDeclarationStatement && initialization.getFirstChild() == initialization.getLastChild() &&
        condition != null && update != null && update.getChildren().size == 1 &&
        (isPlusPlusExpression(update.getChildren()[0])) && (operationTokenType == JavaTokenType.LT || operationTokenType == JavaTokenType.LE) &&
        initialization.getFirstChild() != null && (initialization.getFirstChild() is PsiLocalVariable) &&
        firstChild != null && firstChild.getNameIdentifier() != null && onceWritableIterator) {
            val end = getConverter().convertExpression((condition as PsiBinaryExpression).getROperand())
            val endExpression = (if (operationTokenType == JavaTokenType.LT)
                BinaryExpression(end, Identifier("1"), "-")
            else
                end)
            myResult = ForeachWithRangeStatement(Identifier(firstChild.getName()!!),
                                                 getConverter().convertExpression(firstChild.getInitializer()),
                                                 endExpression,
                                                 getConverter().convertStatement(body))
        }
        else {
            var forStatements = ArrayList<Element>()
            forStatements.add(getConverter().convertStatement(initialization))
            forStatements.add(WhileStatement(
                    if (condition == null)
                        LiteralExpression("true")
                    else
                        getConverter().convertExpression(condition),
                    Block(arrayListOf(getConverter().convertStatement(body),
                                      Block(arrayListOf(getConverter().convertStatement(update)), false)), false)))
            myResult = Block(forStatements, false)
        }
    }

    public override fun visitForeachStatement(statement: PsiForeachStatement?) {
        val iterator = {
            val iteratorExpr = getConverter().convertExpression(statement?.getIteratedValue())
            if (iteratorExpr.isNullable())
                BangBangExpression(iteratorExpr)
            else
                iteratorExpr
        }()
        myResult = ForeachStatement(getConverter().convertParameter(statement?.getIterationParameter()!!),
                                    iterator,
                                    getConverter().convertStatement(statement?.getBody()))
    }

    public override fun visitIfStatement(statement: PsiIfStatement?) {
        val condition: PsiExpression? = statement?.getCondition()
        val expression: Expression = getConverter().convertExpression(condition, PsiType.BOOLEAN)
        myResult = IfStatement(expression,
                               getConverter().convertStatement(statement?.getThenBranch()),
                               getConverter().convertStatement(statement?.getElseBranch()))
    }

    public override fun visitLabeledStatement(statement: PsiLabeledStatement?) {
        myResult = LabelStatement(getConverter().convertIdentifier(statement?.getLabelIdentifier()),
                                  getConverter().convertStatement(statement?.getStatement()))
    }

    public override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement?) {
        myResult = (if (statement?.isDefaultCase()!!)
            DefaultSwitchLabelStatement()
        else
            SwitchLabelStatement(getConverter().convertExpression(statement?.getCaseValue())))
    }

    public override fun visitSwitchStatement(statement: PsiSwitchStatement?) {
        myResult = SwitchContainer(getConverter().convertExpression(statement?.getExpression()),
                                   switchBodyToCases(statement?.getBody()))
    }

    private open fun switchBodyToCases(body: PsiCodeBlock?): List<CaseContainer> {
        val cases: List<List<PsiElement>> = splitToCases(body)
        val allSwitchStatements = ArrayList<PsiElement>()
        if (body != null) {
            // TODO Arrays.asList()
            for (s in body.getStatements()) allSwitchStatements.add(s)
        }
        val result = ArrayList<CaseContainer>()
        var pendingLabels = ArrayList<Element>()
        var i: Int = 0
        var hasDefaultCase: Boolean = false
        for (ls in cases) {
            // TODO assert {(ls?.size()).sure() > 0}
            if (ls.size() > 0) {
                var label = ls[0]
                hasDefaultCase = hasDefaultCase || (label as PsiSwitchLabelStatement).isDefaultCase()
                // TODO assert {(label is PsiSwitchLabelStatement?)}
                // TODO assert("not a right index") {allSwitchStatements?.get(i) == label}
                if (ls.size() > 1) {
                    pendingLabels.add(getConverter().convertStatement(label))
                    val slice: List<PsiElement> = ls.subList(1, (ls.size()))
                    if (!containsBreak(slice)) {
                        val statements = ArrayList(getConverter().convertStatements(slice))
                        statements.addAll(getConverter().convertStatements(getAllToNextBreak(allSwitchStatements, i + ls.size())))
                        result.add(CaseContainer(pendingLabels, statements))
                        pendingLabels = ArrayList()
                    }
                    else {
                        result.add(CaseContainer(pendingLabels, getConverter().convertStatements(slice)))
                        pendingLabels = ArrayList()
                    }
                }
                else {
                    pendingLabels.add(getConverter().convertStatement(label))
                }
                i += ls.size()
            }
        }
        if (!hasDefaultCase)
            result.add(CaseContainer(listOf(DefaultSwitchLabelStatement()), ArrayList()))
        return result
    }

    public override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement?) {
        myResult = SynchronizedStatement(getConverter().convertExpression(statement?.getLockExpression()),
                                         getConverter().convertBlock(statement?.getBody()))
    }

    public override fun visitThrowStatement(statement: PsiThrowStatement?) {
        myResult = ThrowStatement(getConverter().convertExpression(statement?.getException()))
    }

    public override fun visitTryStatement(statement: PsiTryStatement?) {
        val catches = ArrayList<CatchStatement>()
        val catchBlocks = statement?.getCatchBlocks()!!
        val catchBlockParameters = statement?.getCatchBlockParameters()!!
        for (i in 0..catchBlocks.size - 1) {
            catches.add(CatchStatement(getConverter().convertParameter(catchBlockParameters[i], true),
                                       getConverter().convertBlock(catchBlocks[i], true)))
        }
        myResult = TryStatement(getConverter().convertBlock(statement?.getTryBlock(), true),
                                catches, getConverter().convertBlock(statement?.getFinallyBlock(), true))
    }

    public override fun visitWhileStatement(statement: PsiWhileStatement?) {
        var condition: PsiExpression? = statement?.getCondition()
        val expression: Expression = (if (condition != null && condition?.getType() != null)
            this.getConverter().convertExpression(condition, condition?.getType())
        else
            getConverter().convertExpression(condition))
        myResult = WhileStatement(expression, getConverter().convertStatement(statement?.getBody()))
    }

    public override fun visitReturnStatement(statement: PsiReturnStatement?) {
        val returnValue: PsiExpression? = statement?.getReturnValue()
        val methodReturnType: PsiType? = getConverter().methodReturnType
        val expression: Expression = (if (returnValue != null && methodReturnType != null)
            this.getConverter().convertExpression(returnValue, methodReturnType)
        else
            getConverter().convertExpression(returnValue))
        myResult = ReturnStatement(expression)
    }

    class object {
        private open fun isPlusPlusExpression(psiElement: PsiElement): Boolean {
            return (psiElement is PsiPostfixExpression && psiElement.getOperationTokenType() == JavaTokenType.PLUSPLUS) ||
            (psiElement is PsiPrefixExpression && psiElement.getOperationTokenType() == JavaTokenType.PLUSPLUS)
        }

        private fun containsBreak(slice: List<PsiElement?>) = slice.any { it is PsiBreakStatement }

        private open fun getAllToNextBreak(allStatements: List<PsiElement>, start: Int): List<PsiElement> {
            val result = ArrayList<PsiElement>()
            for (i in start..allStatements.size() - 1) {
                val s = allStatements.get(i)
                if (s is PsiBreakStatement || s is PsiReturnStatement) {
                    return result
                }

                if (!(s is PsiSwitchLabelStatement)) {
                    result.add(s)
                }

            }
            return result
        }

        private open fun splitToCases(body: PsiCodeBlock?): List<List<PsiElement>> {
            val cases = ArrayList<List<PsiElement>>()
            var currentCaseStatements = ArrayList<PsiElement>()
            var isFirst: Boolean = true
            if (body != null) {
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
}
