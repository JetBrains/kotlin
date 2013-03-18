package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.*
import java.util.Arrays
import java.util.Collections
import java.util.LinkedList
import org.jetbrains.jet.j2k.countWritingAccesses
import java.util.ArrayList

public open class StatementVisitor(converter: Converter): ElementVisitor(converter) {
    public override fun visitAssertStatement(statement: PsiAssertStatement?): Unit {
        myResult = AssertStatement(getConverter().expressionToExpression(statement?.getAssertCondition()),
                getConverter().expressionToExpression(statement?.getAssertDescription()))
    }

    public override fun visitBlockStatement(statement: PsiBlockStatement?): Unit {
        myResult = myConverter.blockToBlock(statement?.getCodeBlock(), true)
    }

    public override fun visitBreakStatement(statement: PsiBreakStatement?): Unit {
        if (statement?.getLabelIdentifier() == null) {
            myResult = BreakStatement(Identifier.EMPTY_IDENTIFIER)
        }
        else
        {
            myResult = BreakStatement(Converter.identifierToIdentifier(statement?.getLabelIdentifier()))
        }
    }

    public override fun visitContinueStatement(statement: PsiContinueStatement?): Unit {
        if (statement?.getLabelIdentifier() == null)
        {
            myResult = ContinueStatement(Identifier.EMPTY_IDENTIFIER)
        }
        else
        {
            myResult = ContinueStatement(Converter.identifierToIdentifier(statement?.getLabelIdentifier()))
        }
    }

    public override fun visitDeclarationStatement(statement: PsiDeclarationStatement?): Unit {
        myResult = DeclarationStatement(getConverter().elementsToElementList(statement?.getDeclaredElements()!!))
    }

    public override fun visitDoWhileStatement(statement: PsiDoWhileStatement?): Unit {
        val condition: PsiExpression? = statement?.getCondition()
        val expression: Expression = (if (condition != null && condition.getType() != null)
            getConverter().expressionToExpression(condition, condition.getType())
        else
            getConverter().expressionToExpression(condition))
        myResult = DoWhileStatement(expression, getConverter().statementToStatement(statement?.getBody()))
    }

    public override fun visitExpressionStatement(statement: PsiExpressionStatement?): Unit {
        myResult = getConverter().expressionToExpression(statement?.getExpression())
    }

    public override fun visitExpressionListStatement(statement: PsiExpressionListStatement?): Unit {
        myResult = ExpressionListStatement(getConverter().expressionsToExpressionList(
                statement?.getExpressionList()?.getExpressions()!!))
    }

    public override fun visitForStatement(statement: PsiForStatement?): Unit {
        val initialization: PsiStatement? = statement?.getInitialization()
        val update: PsiStatement? = statement?.getUpdate()
        val condition: PsiExpression? = statement?.getCondition()
        val body: PsiStatement? = statement?.getBody()
        val firstChild: PsiLocalVariable? = (if (initialization != null && (initialization.getFirstChild() is PsiLocalVariable))
            (initialization.getFirstChild() as PsiLocalVariable)
        else
            null)
        var bodyWriteCount: Int = countWritingAccesses(firstChild, body)
        var conditionWriteCount: Int = countWritingAccesses(firstChild, condition)
        var updateWriteCount: Int = countWritingAccesses(firstChild, update)
        val onceWritableIterator: Boolean = updateWriteCount == 1 && bodyWriteCount + conditionWriteCount == 0
        val operationTokenType: IElementType? = (if (condition is PsiBinaryExpression)
            condition.getOperationTokenType()
        else
            null)
        if (initialization is PsiDeclarationStatement && initialization.getFirstChild() == initialization.getLastChild() &&
        condition != null && update != null && update.getChildren().size == 1 &&
        (isPlusPlusExpression(update.getChildren()[0])) && (operationTokenType == JavaTokenType.LT || operationTokenType == JavaTokenType.LE) &&
        initialization.getFirstChild() != null && (initialization.getFirstChild() is PsiLocalVariable) &&
        firstChild != null && firstChild.getNameIdentifier() != null && onceWritableIterator) {
            val end: Expression = getConverter().expressionToExpression((condition as PsiBinaryExpression).getROperand())
            val endExpression: Expression = (if (operationTokenType == JavaTokenType.LT)
                BinaryExpression(end, Identifier("1"), "-")
            else
                end)
            myResult = ForeachWithRangeStatement(Identifier(firstChild.getName()!!),
                    getConverter().expressionToExpression(firstChild.getInitializer()),
                    endExpression,
                    getConverter().statementToStatement(body))
        }
        else {
            var forStatements = ArrayList<Element>()
            forStatements.add(getConverter().statementToStatement(initialization))
            forStatements.add(WhileStatement(
                if (condition == null)
                    LiteralExpression("true")
                else
                    getConverter().expressionToExpression(condition),
                Block(arrayListOf(getConverter().statementToStatement(body),
                Block(arrayListOf(getConverter().statementToStatement(update)), false)), false)))
            myResult = Block(forStatements, false)
        }
    }

    public override fun visitForeachStatement(statement: PsiForeachStatement?): Unit {
        val iterator = {
            val iteratorExpr = getConverter().expressionToExpression(statement?.getIteratedValue())
            if (iteratorExpr.isNullable())
                BangBangExpression(iteratorExpr)
            else
                iteratorExpr
        }()
        myResult = ForeachStatement(getConverter().parameterToParameter(statement?.getIterationParameter()!!),
                iterator,
                getConverter().statementToStatement(statement?.getBody()))
    }

    public override fun visitIfStatement(statement: PsiIfStatement?): Unit {
        val condition: PsiExpression? = statement?.getCondition()
        val expression: Expression = getConverter().expressionToExpression(condition, PsiType.BOOLEAN)
        myResult = IfStatement(expression,
                getConverter().statementToStatement(statement?.getThenBranch()),
                getConverter().statementToStatement(statement?.getElseBranch()))
    }

    public override fun visitLabeledStatement(statement: PsiLabeledStatement?): Unit {
        myResult = LabelStatement(Converter.identifierToIdentifier(statement?.getLabelIdentifier()),
                getConverter().statementToStatement(statement?.getStatement()))
    }

    public override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement?): Unit {
        myResult = (if (statement?.isDefaultCase()!!)
            DefaultSwitchLabelStatement()
        else
            SwitchLabelStatement(getConverter().expressionToExpression(statement?.getCaseValue())))
    }

    public override fun visitSwitchStatement(statement: PsiSwitchStatement?): Unit {
        myResult = SwitchContainer(getConverter().expressionToExpression(statement?.getExpression()),
                switchBodyToCases(statement?.getBody()))
    }

    private open fun switchBodyToCases(body: PsiCodeBlock?): List<CaseContainer> {
        val cases: List<List<PsiElement>> = splitToCases(body)
        val allSwitchStatements = ArrayList<PsiElement>()
        if (body != null) {
            // TODO Arrays.asList()
            for(s in body.getStatements()) allSwitchStatements.add(s)
        }
        val result = ArrayList<CaseContainer>()
        var pendingLabels = ArrayList<Element>()
        var i: Int = 0
        for (ls in cases) {
            // TODO assert {(ls?.size()).sure() > 0}
            var label = ls[0]
            // TODO assert {(label is PsiSwitchLabelStatement?)}
            // TODO assert("not a right index") {allSwitchStatements?.get(i) == label}
            if (ls.size() > 1) {
                pendingLabels.add(getConverter().statementToStatement(label))
                val slice: List<PsiElement> = ls.subList(1, (ls.size()))
                if (!containsBreak(slice)) {
                    val statements = ArrayList(getConverter().statementsToStatementList(slice))
                    statements.addAll(getConverter().statementsToStatementList(getAllToNextBreak(allSwitchStatements, i + ls.size())))
                    result.add(CaseContainer(pendingLabels, statements))
                    pendingLabels = arrayList()
                }
                else {
                    result.add(CaseContainer(pendingLabels, getConverter().statementsToStatementList(slice)))
                    pendingLabels = arrayList()
                }
            }
            else {
                pendingLabels.add(getConverter().statementToStatement(label))
            }
            i += ls.size()
        }
        return result
    }

    public override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement?): Unit {
        myResult = SynchronizedStatement(getConverter().expressionToExpression(statement?.getLockExpression()),
                getConverter().blockToBlock(statement?.getBody()))
    }

    public override fun visitThrowStatement(statement: PsiThrowStatement?): Unit {
        myResult = ThrowStatement(getConverter().expressionToExpression(statement?.getException()))
    }

    public override fun visitTryStatement(statement: PsiTryStatement?): Unit {
        val catches = ArrayList<CatchStatement>()
        val catchBlocks = statement?.getCatchBlocks()!!
        val catchBlockParameters = statement?.getCatchBlockParameters()!!
        for (i in 0..catchBlocks.size - 1) {
            catches.add(CatchStatement(getConverter().parameterToParameter(catchBlockParameters[i], true),
                    getConverter().blockToBlock(catchBlocks[i], true)))
        }
        myResult = TryStatement(getConverter().blockToBlock(statement?.getTryBlock(), true),
                catches, getConverter().blockToBlock(statement?.getFinallyBlock(), true))
    }

    public override fun visitWhileStatement(statement: PsiWhileStatement?): Unit {
        var condition: PsiExpression? = statement?.getCondition()
        val expression: Expression = (if (condition != null && condition?.getType() != null)
            this.getConverter().expressionToExpression(condition, condition?.getType())
        else
            getConverter().expressionToExpression(condition))
        myResult = WhileStatement(expression, getConverter().statementToStatement(statement?.getBody()))
    }

    public override fun visitReturnStatement(statement: PsiReturnStatement?): Unit {
        val returnValue: PsiExpression? = statement?.getReturnValue()
        val methodReturnType: PsiType? = getConverter().getMethodReturnType()
        val expression: Expression = (if (returnValue != null && methodReturnType != null)
            this.getConverter().expressionToExpression(returnValue, methodReturnType)
        else
            getConverter().expressionToExpression(returnValue))
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
                            currentCaseStatements = arrayList()
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
