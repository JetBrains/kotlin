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
import org.jetbrains.jet.j2k.hasWriteAccesses
import org.jetbrains.jet.j2k.isInSingleLine
import org.jetbrains.jet.j2k.getContainingMethod

open class StatementVisitor(public val converter: Converter) : JavaElementVisitor() {
    public var result: Statement = Statement.Empty
        protected set

    public fun reset() {
        result = Statement.Empty
    }

    override fun visitAssertStatement(statement: PsiAssertStatement) {
        val descriptionExpr = statement.getAssertDescription()
        val condition = converter.convertExpression(statement.getAssertCondition())
        if (descriptionExpr == null) {
            result = MethodCallExpression.buildNotNull(null, "assert", listOf(condition))
        }
        else {
            val description = converter.convertExpression(descriptionExpr)
            if (descriptionExpr is PsiLiteralExpression) {
                result = MethodCallExpression.buildNotNull(null, "assert", listOf(condition, description))
            }
            else {
                val block = Block(listOf(description), LBrace().assignNoPrototype(), RBrace().assignNoPrototype())
                val lambda = LambdaExpression(null, block.assignNoPrototype())
                result = MethodCallExpression.build(null, "assert", listOf(condition), listOf(), false, lambda)
            }
        }
    }

    override fun visitBlockStatement(statement: PsiBlockStatement) {
        val block = converter.convertBlock(statement.getCodeBlock())
        result = MethodCallExpression.build(null, "run", listOf(), listOf(), false, LambdaExpression(null, block).assignNoPrototype())
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
        result = DeclarationStatement(statement.getDeclaredElements().map { converter.convertElement(it) })
    }

    override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
        val condition = statement.getCondition()
        val expression = if (condition != null && condition.getType() != null)
            converter.convertExpression(condition, condition.getType())
        else
            converter.convertExpression(condition)
        result = DoWhileStatement(expression, convertStatementOrBlock(statement.getBody()), statement.isInSingleLine())
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

        val loopVar = initialization?.getFirstChild() as? PsiLocalVariable
        val onceWritableIterator = loopVar != null
                && !loopVar.hasWriteAccesses(body)
                && !loopVar.hasWriteAccesses(condition)
                && loopVar.countWriteAccesses(update) == 1

        val operationTokenType = (condition as? PsiBinaryExpression)?.getOperationTokenType()
        if (initialization is PsiDeclarationStatement
                && initialization.getFirstChild() == initialization.getLastChild()
                && condition != null
                && update != null
                && update.getChildren().size == 1
                && update.getChildren().single().isPlusPlusExpression()
                && (operationTokenType == JavaTokenType.LT || operationTokenType == JavaTokenType.LE)
                && loopVar != null
                && loopVar.getNameIdentifier() != null
                && onceWritableIterator) {
            val end = converter.convertExpression((condition as PsiBinaryExpression).getROperand())
            val endExpression = if (operationTokenType == JavaTokenType.LT)
                BinaryExpression(end, LiteralExpression("1").assignNoPrototype(), "-").assignNoPrototype()
            else
                end
            result = ForeachWithRangeStatement(loopVar.declarationIdentifier(),
                                                 converter.convertExpression(loopVar.getInitializer()),
                                                 endExpression,
                                                 convertStatementOrBlock(body),
                                                 statement.isInSingleLine())
        }
        else {
            val initializationConverted = converter.convertStatement(initialization)
            val updateConverted = converter.convertStatement(update)

            val whileBody = if (updateConverted.isEmpty) {
                convertStatementOrBlock(body)
            }
            else if (body is PsiBlockStatement) {
                val nameConflict = initialization is PsiDeclarationStatement && initialization.getDeclaredElements().any { loopVar ->
                    loopVar is PsiNamedElement && body.getCodeBlock().getStatements().any { statement ->
                        statement is PsiDeclarationStatement && statement.getDeclaredElements().any {
                            it is PsiNamedElement && it.getName() == loopVar.getName()
                        }
                    }
                }

                if (nameConflict) {
                    val statements = listOf(converter.convertStatement(body), updateConverted)
                    Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype(), true).assignNoPrototype()
                }
                else {
                    val block = converter.convertBlock(body.getCodeBlock(), true)
                    Block(block.statements + listOf(updateConverted), block.lBrace, block.rBrace, true).assignPrototypesFrom(block)
                }
            }
            else {
                val statements = listOf(converter.convertStatement(body), updateConverted)
                Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype(), true).assignNoPrototype()
            }
            val whileStatement = WhileStatement(
                    if (condition != null) converter.convertExpression(condition) else LiteralExpression("true").assignNoPrototype(),
                    whileBody,
                    statement.isInSingleLine()).assignNoPrototype()

            if (initializationConverted.isEmpty) {
                result = whileStatement
            }
            else {
                val statements = listOf(initializationConverted, whileStatement)
                val block = Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype()).assignNoPrototype()
                result = MethodCallExpression.build(null, "run", listOf(), listOf(), false, LambdaExpression(null, block))
            }
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
                                  convertStatementOrBlock(statement.getBody()),
                                  statement.isInSingleLine())
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        val condition = statement.getCondition()
        val expression = converter.convertExpression(condition, PsiType.BOOLEAN)
        result = IfStatement(expression,
                             convertStatementOrBlock(statement.getThenBranch()),
                             convertStatementOrBlock(statement.getElseBranch()),
                             statement.isInSingleLine())
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
        var hasDefaultCase = false
        for (ls in cases) {
            if (ls.size() > 0) {
                var label = ls[0] as PsiSwitchLabelStatement
                hasDefaultCase = hasDefaultCase || label.isDefaultCase()
                // TODO assert {(label is PsiSwitchLabelStatement?)}
                // TODO assert("not a right index") {allSwitchStatements?.get(i) == label}
                if (ls.size() > 1) {
                    pendingLabels.add(converter.convertStatement(label))
                    val slice = ls.subList(1, (ls.size()))

                    fun convertStatements(elements: List<PsiElement>): List<Statement>
                            = elements.map { if (it is PsiStatement) converter.convertStatement(it) else null }.filterNotNull()

                    if (!containsBreak(slice)) {
                        val statements = convertStatements(slice) + convertStatements(getAllToNextBreak(allSwitchStatements, i + ls.size()))
                        result.add(CaseContainer(pendingLabels, statements).assignNoPrototype())
                    }
                    else {
                        result.add(CaseContainer(pendingLabels, convertStatements(slice)).assignNoPrototype())
                    }
                    pendingLabels = ArrayList()
                }
                else {
                    pendingLabels.add(converter.convertStatement(label))
                }
                i += ls.size()
            }
        }
        if (!hasDefaultCase) {
            result.add(CaseContainer(listOf(DefaultSwitchLabelStatement().assignNoPrototype()), listOf()).assignNoPrototype())
        }
        return result
    }

    override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement) {
        result = SynchronizedStatement(converter.convertExpression(statement.getLockExpression()),
                                         converter.convertBlock(statement.getBody()))
    }

    override fun visitThrowStatement(statement: PsiThrowStatement) {
        result = ThrowStatement(converter.convertExpression(statement.getException()))
    }

    override fun visitTryStatement(tryStatement: PsiTryStatement) {
        val tryBlock = tryStatement.getTryBlock()
        val catchesConverted = run {
            val catchBlocks = tryStatement.getCatchBlocks()
            val catchBlockParameters = tryStatement.getCatchBlockParameters()
            catchBlocks.indices.map {
                CatchStatement(converter.convertParameter(catchBlockParameters[it], Nullability.NotNull),
                               converter.convertBlock(catchBlocks[it])).assignNoPrototype()
            }
        }
        val finallyConverted = converter.convertBlock(tryStatement.getFinallyBlock())

        val resourceList = tryStatement.getResourceList()
        if (resourceList != null) {
            val variables = resourceList.getResourceVariables()
            if (variables.isNotEmpty()) {
                result = convertTryWithResources(tryBlock, variables, catchesConverted, finallyConverted)
                return
            }
        }

        result = TryStatement(converter.convertBlock(tryBlock), catchesConverted, finallyConverted)
    }

    private fun convertTryWithResources(tryBlock: PsiCodeBlock?, resourceVariables: List<PsiResourceVariable>, catchesConverted: List<CatchStatement>, finallyConverted: Block): Statement {
        var wrapResultStatement: (Expression) -> Statement = { it }
        var converterForBody = converter

        val returns = collectReturns(tryBlock)
        //TODO: support other returns when non-local returns supported by Kotlin
        if (returns.size == 1 && returns.single() == tryBlock!!.getStatements().last()) {
            wrapResultStatement = { ReturnStatement(it).assignPrototype(returns.single()) }
            converterForBody = converter.withStatementVisitor { object : StatementVisitor(it) {
                override fun visitReturnStatement(statement: PsiReturnStatement) {
                    if (statement == returns.single()) {
                        result = converter.convertExpression(statement.getReturnValue(), tryBlock!!.getContainingMethod()?.getReturnType())
                    }
                    else {
                        super.visitReturnStatement(statement)
                    }
                }
            }}
        }

        var block = converterForBody.convertBlock(tryBlock)
        var expression: Expression = Expression.Empty
        for (variable in resourceVariables.reverse()) {
            val lambda = LambdaExpression(Identifier.toKotlin(variable.getName()!!), block)
            expression = MethodCallExpression.build(converter.convertExpression(variable.getInitializer()), "use", listOf(), listOf(), false, lambda)
            expression.assignNoPrototype()
            block = Block(listOf(expression), LBrace().assignNoPrototype(), RBrace().assignNoPrototype()).assignNoPrototype()
        }

        if (catchesConverted.isEmpty() && finallyConverted.isEmpty) {
            return wrapResultStatement(expression)
        }

        block = Block(listOf(wrapResultStatement(expression)), LBrace().assignPrototype(tryBlock?.getLBrace()), RBrace().assignPrototype(tryBlock?.getRBrace()), true)
        return TryStatement(block.assignPrototype(tryBlock), catchesConverted, finallyConverted)
    }

    private fun collectReturns(block: PsiCodeBlock?): Collection<PsiReturnStatement> {
        val returns = ArrayList<PsiReturnStatement>()
        block?.accept(object: JavaRecursiveElementVisitor() {
            override fun visitReturnStatement(statement: PsiReturnStatement) {
                returns.add(statement)
            }

            override fun visitMethod(method: PsiMethod) {
                // do not go inside any other method (e.g. in anonymous class)
            }
        })
        return returns
    }

    override fun visitWhileStatement(statement: PsiWhileStatement) {
        val condition = statement.getCondition()
        val expression = if (condition?.getType() != null)
            converter.convertExpression(condition, condition!!.getType())
        else
            converter.convertExpression(condition)
        result = WhileStatement(expression, convertStatementOrBlock(statement.getBody()), statement.isInSingleLine())
    }

    override fun visitReturnStatement(statement: PsiReturnStatement) {
        val returnValue = statement.getReturnValue()
        val methodReturnType = converter.methodReturnType
        val expression = if (returnValue != null && methodReturnType != null)
            converter.convertExpression(returnValue, methodReturnType)
        else
            converter.convertExpression(returnValue)
        result = ReturnStatement(expression)
    }

    override fun visitEmptyStatement(statement: PsiEmptyStatement) {
        result = Statement.Empty
    }

    private fun PsiElement.isPlusPlusExpression(): Boolean {
        return (this is PsiPostfixExpression && this.getOperationTokenType() == JavaTokenType.PLUSPLUS) ||
                (this is PsiPrefixExpression && this.getOperationTokenType() == JavaTokenType.PLUSPLUS)
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

    private fun convertStatementOrBlock(statement: PsiStatement?): Statement {
        return if (statement is PsiBlockStatement)
            converter.convertBlock(statement.getCodeBlock())
        else
            converter.convertStatement(statement)
    }
}
