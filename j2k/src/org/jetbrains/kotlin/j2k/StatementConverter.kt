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
import org.jetbrains.kotlin.j2k.ast.*
import java.util.*

interface StatementConverter {
    fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement
}

interface SpecialStatementConverter {
    fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement?
}

fun StatementConverter.withSpecialConverter(specialConverter: SpecialStatementConverter): StatementConverter {
    return object: StatementConverter {
        override fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement
                = specialConverter.convertStatement(statement, codeConverter) ?: this@withSpecialConverter.convertStatement(statement, codeConverter)
    }
}

class DefaultStatementConverter : JavaElementVisitor(), StatementConverter {
    private var _codeConverter: CodeConverter? = null
    private var result: Statement = Statement.Empty

    private val codeConverter: CodeConverter get() = _codeConverter!!
    private val converter: Converter get() = codeConverter.converter

    override fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement {
        this._codeConverter = codeConverter
        result = Statement.Empty

        statement.accept(this)
        return result
    }

    override fun visitAssertStatement(statement: PsiAssertStatement) {
        val descriptionExpr = statement.assertDescription
        val condition = codeConverter.convertExpression(statement.assertCondition)
        if (descriptionExpr == null) {
            result = MethodCallExpression.buildNonNull(null, "assert", ArgumentList.withNoPrototype(condition))
        }
        else {
            val description = codeConverter.convertExpression(descriptionExpr)
            val lambda = LambdaExpression(null, Block.of(description).assignNoPrototype())
            result = MethodCallExpression.buildNonNull(null, "assert", ArgumentList.withNoPrototype(condition, lambda))
        }
    }

    override fun visitBlockStatement(statement: PsiBlockStatement) {
        val block = codeConverter.convertBlock(statement.codeBlock)
        result = MethodCallExpression.buildNonNull(null, "run", ArgumentList.withNoPrototype(LambdaExpression(null, block).assignNoPrototype()))
    }

    override fun visitBreakStatement(statement: PsiBreakStatement) {
        if (statement.labelIdentifier == null) {
            result = BreakStatement(Identifier.Empty)
        }
        else {
            result = BreakStatement(converter.convertIdentifier(statement.labelIdentifier))
        }
    }

    override fun visitContinueStatement(statement: PsiContinueStatement) {
        if (statement.labelIdentifier == null) {
            result = ContinueStatement(Identifier.Empty)
        }
        else {
            result = ContinueStatement(converter.convertIdentifier(statement.labelIdentifier))
        }
    }

    override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
        result = DeclarationStatement(statement.declaredElements.map {
            when (it) {
                is PsiLocalVariable -> codeConverter.convertLocalVariable(it)
                is PsiClass -> converter.convertClass(it)
                else -> Element.Empty //what else can be here?
            }
        })
    }

    override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
        val condition = statement.condition
        val expression = if (condition != null && condition.type != null)
            codeConverter.convertExpression(condition, condition.type)
        else
            codeConverter.convertExpression(condition)
        result = DoWhileStatement(expression, codeConverter.convertStatementOrBlock(statement.body), statement.isInSingleLine())
    }

    override fun visitExpressionStatement(statement: PsiExpressionStatement) {
        result = codeConverter.convertExpression(statement.expression)
    }

    override fun visitExpressionListStatement(statement: PsiExpressionListStatement) {
        result = ExpressionListStatement(codeConverter.convertExpressionsInList(statement.expressionList.expressions.asList()))
    }

    override fun visitForStatement(statement: PsiForStatement) {
        result = ForConverter(statement, codeConverter).execute()
    }

    override fun visitForeachStatement(statement: PsiForeachStatement) {
        val iterator = codeConverter.convertExpression(statement.iteratedValue, null, Nullability.NotNull)
        val iterationParameter = statement.iterationParameter
        result = ForeachStatement(iterationParameter.declarationIdentifier(),
                                  if (codeConverter.settings.specifyLocalVariableTypeByDefault) codeConverter.typeConverter.convertVariableType(iterationParameter) else null,
                                  iterator,
                                  codeConverter.convertStatementOrBlock(statement.body),
                                  statement.isInSingleLine())
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        val condition = statement.condition
        val expression = codeConverter.convertExpression(condition, PsiType.BOOLEAN)
        result = IfStatement(expression,
                             codeConverter.convertStatementOrBlock(statement.thenBranch),
                             codeConverter.convertStatementOrBlock(statement.elseBranch),
                             statement.isInSingleLine())
    }

    override fun visitLabeledStatement(statement: PsiLabeledStatement) {
        val statementConverted = codeConverter.convertStatement(statement.statement)
        val identifier = converter.convertIdentifier(statement.labelIdentifier)
        if (statementConverted is ForConverter.WhileWithInitializationPseudoStatement) { // special case - if our loop gets converted to while with initialization we should move the label to the loop
            val labeledLoop = LabeledStatement(identifier, statementConverted.loop).assignPrototype(statement)
            result = ForConverter.WhileWithInitializationPseudoStatement(statementConverted.initialization, labeledLoop, statementConverted.kind)
        }
        else {
            result = LabeledStatement(identifier, statementConverted)
        }
    }

    override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement) {
        result = if (statement.isDefaultCase)
            ElseWhenEntrySelector()
        else
            ValueWhenEntrySelector(codeConverter.convertExpression(statement.caseValue))
    }

    override fun visitSwitchStatement(statement: PsiSwitchStatement) {
        result = SwitchConverter(codeConverter).convert(statement)
    }

    override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement) {
        result = SynchronizedStatement(codeConverter.convertExpression(statement.lockExpression),
                                       codeConverter.convertBlock(statement.body))
    }

    override fun visitThrowStatement(statement: PsiThrowStatement) {
        result = ThrowStatement(codeConverter.convertExpression(statement.exception))
    }

    override fun visitTryStatement(tryStatement: PsiTryStatement) {
        val tryBlock = tryStatement.tryBlock
        val catchesConverted = convertCatches(tryStatement)
        val finallyConverted = codeConverter.convertBlock(tryStatement.finallyBlock)

        val resourceList = tryStatement.resourceList
        if (resourceList != null) {
            val variables = resourceList.resourceVariables
            if (variables.isNotEmpty()) {
                result = convertTryWithResources(tryBlock, variables, catchesConverted, finallyConverted)
                return
            }
        }

        result = TryStatement(codeConverter.convertBlock(tryBlock), catchesConverted, finallyConverted)
    }

    private fun convertCatches(tryStatement: PsiTryStatement): List<CatchStatement> {
        val catches = ArrayList<CatchStatement>()
        for ((block, parameter) in tryStatement.catchBlocks.zip(tryStatement.catchBlockParameters)) {
            val blockConverted = codeConverter.convertBlock(block)
            val annotations = converter.convertAnnotations(parameter)
            val parameterType = parameter.type
            val types = if (parameterType is PsiDisjunctionType)
                parameterType.disjunctions
            else
                listOf(parameterType)
            for (t in types) {
                val convertedType = codeConverter.typeConverter.convertType(t, Nullability.NotNull)
                val convertedParameter = FunctionParameter(parameter.declarationIdentifier(),
                                                           convertedType,
                                                           FunctionParameter.VarValModifier.None,
                                                           annotations,
                                                           Modifiers.Empty).assignPrototype(parameter)
                catches.add(CatchStatement(convertedParameter, blockConverted).assignNoPrototype())
            }
        }
        return catches
    }

    private fun convertTryWithResources(tryBlock: PsiCodeBlock?, resourceVariables: List<PsiResourceVariable>, catchesConverted: List<CatchStatement>, finallyConverted: Block): Statement {
        val wrapResultStatement: (Expression) -> Statement = { it }
        val converterForBody = codeConverter

        var block = converterForBody.convertBlock(tryBlock)
        var expression: Expression = Expression.Empty
        for (variable in resourceVariables.asReversed()) {
            val parameter = LambdaParameter(Identifier.withNoPrototype(variable.name!!), null).assignNoPrototype()
            val parameterList = ParameterList(listOf(parameter), lPar = null, rPar = null).assignNoPrototype()
            val lambda = LambdaExpression(parameterList, block)
            expression = MethodCallExpression.buildNonNull(codeConverter.convertExpression(variable.initializer), "use", ArgumentList.withNoPrototype(lambda))
            expression.assignNoPrototype()
            block = Block.of(expression).assignNoPrototype()
        }

        if (catchesConverted.isEmpty() && finallyConverted.isEmpty) {
            return wrapResultStatement(expression)
        }

        block = Block(listOf(wrapResultStatement(expression)), LBrace().assignPrototype(tryBlock?.lBrace), RBrace().assignPrototype(tryBlock?.rBrace), true)
        return TryStatement(block.assignPrototype(tryBlock), catchesConverted, finallyConverted)
    }

    override fun visitWhileStatement(statement: PsiWhileStatement) {
        val condition = statement.condition
        val expression = if (condition?.type != null)
            codeConverter.convertExpression(condition, condition!!.type)
        else
            codeConverter.convertExpression(condition)
        result = WhileStatement(expression, codeConverter.convertStatementOrBlock(statement.body), statement.isInSingleLine())
    }

    override fun visitReturnStatement(statement: PsiReturnStatement) {
        val returnValue = statement.returnValue
        val methodReturnType = codeConverter.methodReturnType
        val expression = if (returnValue != null && methodReturnType != null)
            codeConverter.convertExpression(returnValue, methodReturnType)
        else
            codeConverter.convertExpression(returnValue)

        result = ReturnStatement(expression)
    }

    override fun visitEmptyStatement(statement: PsiEmptyStatement) {
        result = Statement.Empty
    }
}

fun CodeConverter.convertStatementOrBlock(statement: PsiStatement?): Statement {
    return if (statement is PsiBlockStatement)
        convertBlock(statement.codeBlock)
    else
        convertStatement(statement)
}

