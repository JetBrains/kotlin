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
import java.util.ArrayList
import org.jetbrains.jet.j2k.isInSingleLine

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
        result = DeclarationStatement(statement.getDeclaredElements().map {
            when (it) {
                is PsiLocalVariable -> converter.convertLocalVariable(it)
                is PsiClass -> converter.convertClass(it)
                else -> Element.Empty //what else can be here?
            }
        })
    }

    override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
        val condition = statement.getCondition()
        val expression = if (condition != null && condition.getType() != null)
            converter.convertExpression(condition, condition.getType())
        else
            converter.convertExpression(condition)
        result = DoWhileStatement(expression, converter.convertStatementOrBlock(statement.getBody()), statement.isInSingleLine())
    }

    override fun visitExpressionStatement(statement: PsiExpressionStatement) {
        result = converter.convertExpression(statement.getExpression())
    }

    override fun visitExpressionListStatement(statement: PsiExpressionListStatement) {
        result = ExpressionListStatement(converter.convertExpressions(statement.getExpressionList().getExpressions()))
    }

    override fun visitForStatement(statement: PsiForStatement) {
        result = ForConverter(statement, converter).execute()
    }

    override fun visitForeachStatement(statement: PsiForeachStatement) {
        val iteratorExpr = converter.convertExpression(statement.getIteratedValue())
        val iterator = if (iteratorExpr.isNullable) BangBangExpression(iteratorExpr).assignNoPrototype() else iteratorExpr
        val iterationParameter = statement.getIterationParameter()
        result = ForeachStatement(iterationParameter.declarationIdentifier(),
                                  if (converter.settings.specifyLocalVariableTypeByDefault) converter.typeConverter.convertVariableType(iterationParameter) else null,
                                  iterator,
                                  converter.convertStatementOrBlock(statement.getBody()),
                                  statement.isInSingleLine())
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        val condition = statement.getCondition()
        val expression = converter.convertExpression(condition, PsiType.BOOLEAN)
        result = IfStatement(expression,
                             converter.convertStatementOrBlock(statement.getThenBranch()),
                             converter.convertStatementOrBlock(statement.getElseBranch()),
                             statement.isInSingleLine())
    }

    override fun visitLabeledStatement(statement: PsiLabeledStatement) {
        result = LabelStatement(converter.convertIdentifier(statement.getLabelIdentifier()),
                                converter.convertStatement(statement.getStatement()))
    }

    override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement) {
        result = if (statement.isDefaultCase())
            ElseWhenEntrySelector()
        else
            ValueWhenEntrySelector(converter.convertExpression(statement.getCaseValue()))
    }

    override fun visitSwitchStatement(statement: PsiSwitchStatement) {
        result = SwitchConverter(converter).convert(statement)
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
        val catchesConverted = convertCatches(tryStatement)
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

    private fun convertCatches(tryStatement: PsiTryStatement): List<CatchStatement> {
        val catches = ArrayList<CatchStatement>()
        for ((block, parameter) in tryStatement.getCatchBlocks().zip(tryStatement.getCatchBlockParameters())) {
            val blockConverted = converter.convertBlock(block)
            val annotations = converter.convertAnnotations(parameter)
            val parameterType = parameter.getType()
            val types = if (parameterType is PsiDisjunctionType)
                parameterType.getDisjunctions()
            else
                listOf(parameterType)
            for (t in types) {
                var convertedType = converter.typeConverter.convertType(t, Nullability.NotNull)
                val convertedParameter = Parameter(parameter.declarationIdentifier(),
                                                   convertedType,
                                                   Parameter.VarValModifier.None,
                                                   annotations,
                                                   Modifiers.Empty).assignPrototype(parameter)
                catches.add(CatchStatement(convertedParameter, blockConverted).assignNoPrototype())
            }
        }
        return catches
    }

    private fun convertTryWithResources(tryBlock: PsiCodeBlock?, resourceVariables: List<PsiResourceVariable>, catchesConverted: List<CatchStatement>, finallyConverted: Block): Statement {
        var wrapResultStatement: (Expression) -> Statement = { it }
        var converterForBody = converter

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
        result = WhileStatement(expression, converter.convertStatementOrBlock(statement.getBody()), statement.isInSingleLine())
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
}

fun Converter.convertStatementOrBlock(statement: PsiStatement?): Statement {
    return if (statement is PsiBlockStatement)
        convertBlock(statement.getCodeBlock())
    else
        convertStatement(statement)
}

