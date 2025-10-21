/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.coverage.compiler.instrumentation

import org.jetbrains.kotlin.coverage.compiler.common.KotlinCoverageInstrumentationContext
import org.jetbrains.kotlin.coverage.compiler.hit.BlockWithExecutionPoints
import org.jetbrains.kotlin.coverage.compiler.hit.HitRegistrar
import org.jetbrains.kotlin.coverage.compiler.metadata.FunctionIM
import org.jetbrains.kotlin.coverage.compiler.metadata.LineBranchBodyIM
import org.jetbrains.kotlin.coverage.compiler.metadata.LineBranchBodyIM.LineInfo
import org.jetbrains.kotlin.coverage.compiler.metadata.Position
import org.jetbrains.kotlin.coverage.compiler.metadata.position
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.Name

internal class LineBranchInstrumenter() : Instrumenter {
    override fun instrument(
        irFunction: IrFunction,
        functionIM: FunctionIM,
        irFile: IrFile,
        hitRegistrar: HitRegistrar,
        context: KotlinCoverageInstrumentationContext,
    ) {
        val body = irFunction.body ?: return

        val bodyPointsRegistry = hitRegistrar.body(irFunction)

        when (body) {
            is IrBlockBody -> instrumentBlock(body, irFunction, functionIM, bodyPointsRegistry, irFile, context)
            is IrExpressionBody -> {}
            else -> {}// do nothing
        }
    }


    private fun instrumentBlock(
        block: IrBlockBody,
        irFunction: IrFunction,
        functionIM: FunctionIM,
        pointsRegistry: BlockWithExecutionPoints,
        irFile: IrFile,
        context: KotlinCoverageInstrumentationContext,
    ) {
        val bodyInstrumentation = BodyInstrumentation(pointsRegistry, irFile, irFunction, context)
        val instrumented = bodyInstrumentation.instrument(block.statements)

        if (bodyInstrumentation.finishedLines.isNotEmpty()) {
            block.statements.clear()
            block.statements.addAll(instrumented)
            functionIM.body = LineBranchBodyIM(bodyInstrumentation.finishedLines)
        }
    }
}


private class BodyInstrumentation(
    val pointsRegistry: BlockWithExecutionPoints,
    val irFile: IrFile,
    val parentFunction: IrDeclarationParent,
    val context: KotlinCoverageInstrumentationContext,
) {
    var currentLine: LineInfo? = null
    var tmpVariableCount = 0

    val finishedLines: MutableList<LineInfo> = mutableListOf()
    val currentBranches: MutableList<LineBranchBodyIM.BranchInfo> = mutableListOf()

    fun instrument(statements: List<IrStatement>): List<IrStatement> {
        val instrumentedStatements = mutableListOf<IrStatement>()

        instrumentedStatements.add(pointsRegistry.firstStatement)
        statements.forEach { statement ->
            instrumentedStatements.addAll(instrumentStatement(statement).allStatements)
        }

        if (currentLine != null) {
            finishedLines.add(currentLine!!)
        }

        return instrumentedStatements
    }


    private fun instrumentStatement(statement: IrStatement): InstrumentationResult {
        return when (statement) {
            is IrReturn -> instrument(statement)
            is IrConst -> instrument(statement)
            is IrVariable -> instrument(statement)
            is IrSetValue -> instrument(statement)
//            is IrGetValue -> instrument(statement)
            is IrCall -> instrument(statement)
            is IrConstructorCall -> instrument(statement)
            is IrTypeOperatorCall -> instrument(statement)
            else -> {
                InstrumentationResult.fromStatement(statement)
            }
        }
    }

    private fun instrument(returnStatement: IrReturn): InstrumentationResult {
        val hitPointStatement = registerLinePoint(returnStatement)
        returnStatement.value = instrumentStatement(returnStatement.value).wrap()
        return InstrumentationResult.fromPointed(hitPointStatement, returnStatement)
    }

    private fun instrument(constStatement: IrConst): InstrumentationResult {
        val hitPointStatement = registerLinePoint(constStatement)
        return InstrumentationResult.fromPointed(hitPointStatement, constStatement)
    }

    private fun instrument(variable: IrVariable): InstrumentationResult {
        if (variable.origin != IrDeclarationOrigin.DEFINED) {
            // process only user-defined variables
            return InstrumentationResult.fromStatement(variable)
        }

        val hitPointStatement = registerLinePoint(variable)
        variable.initializer = variable.initializer?.let { instrumentStatement(it).wrap() }
        return InstrumentationResult.fromPointed(hitPointStatement, variable)
    }

    private fun instrument(setValue: IrSetValue): InstrumentationResult {
        if (setValue.origin != IrStatementOrigin.EQ) {
            // process only user-defined variables
            return InstrumentationResult.fromStatement(setValue)
        }

        val hitPointStatement = registerLinePoint(setValue)
        setValue.value = instrumentStatement(setValue.value).wrap()
        return InstrumentationResult.fromPointed(hitPointStatement, setValue)
    }

    private fun instrument(irCall: IrCall): InstrumentationResult {
        val leadingStatements = mutableListOf<IrStatement>()
        // process receivers
        irCall.symbol.owner.parameters.forEachIndexed { index, parameter ->
            if (parameter.kind != IrParameterKind.DispatchReceiver && parameter.kind != IrParameterKind.ExtensionReceiver) {
                return@forEachIndexed
            }
            // skip null arguments (default values in most cases)
            val arg = irCall.arguments[index] ?: return@forEachIndexed

            val instrumentedReceiver = instrumentReceiver(arg)

            irCall.arguments[index] = instrumentedReceiver.asExpression
            // add initialization of receivers in separate variables
            leadingStatements.addAll(instrumentedReceiver.leadingStatements)
        }

        val hitPointStatement = registerLinePoint(irCall)

        // process arguments
        irCall.symbol.owner.parameters.forEachIndexed { index, parameter ->
            if (parameter.kind != IrParameterKind.Regular) {
                return@forEachIndexed
            }
            // skip null arguments - in [actualArgs] it's already null
            val arg = irCall.arguments[index] ?: return@forEachIndexed
            irCall.arguments[index] = instrumentStatement(arg).wrap()
        }

        return InstrumentationResult.fromPointed(leadingStatements, hitPointStatement, irCall)
    }

    private fun instrument(irConstructorCall: IrConstructorCall): InstrumentationResult {
        val hitPointStatement = registerLinePoint(irConstructorCall)

        // process arguments
        irConstructorCall.symbol.owner.parameters.forEachIndexed { index, parameter ->
            if (parameter.kind != IrParameterKind.Regular) {
                return@forEachIndexed
            }
            // skip null arguments - in [actualArgs] it's already null
            val arg = irConstructorCall.arguments[index] ?: return@forEachIndexed
            irConstructorCall.arguments[index] = instrumentStatement(arg).wrap()
        }
        return InstrumentationResult.fromPointed(hitPointStatement, irConstructorCall)
    }

    private fun instrument(irTypeOperatorCall: IrTypeOperatorCall): InstrumentationResult {
        val instrumented = instrumentStatement(irTypeOperatorCall.argument).wrap()
        irTypeOperatorCall.argument = instrumented
        // don't change the operator itself
        return InstrumentationResult.fromStatement(irTypeOperatorCall)
    }

    private fun instrumentReceiver(irExpression: IrExpression): InstrumentationResult {
        val name = Name.identifier($$"$tmp" + tmpVariableCount++)
        val instrumented = instrumentStatement(irExpression)
        return when {
            instrumented.hasHitPoint && instrumented.leadingStatements.isEmpty() ->
                InstrumentationResult.fromPointed(listOf(instrumented.hitPoint), null, instrumented.statement)

            instrumented.hasHitPoint && instrumented.leadingStatements.isNotEmpty() -> {
                val variable = context.factory.`val`(name, irExpression.type, parentFunction, instrumented.asExpression)
                val get = context.factory.getValue(variable)
                InstrumentationResult.fromPointed(instrumented.leadingStatements + variable + instrumented.hitPoint, null, get)
            }

            else -> return instrumented
        }
    }

    /**
     * Add execution point before [irStatement] if needed.
     */
    private fun registerLinePoint(irStatement: IrStatement): IrStatement? {
        val position = irFile.position(irStatement.startOffset)
        return if (isNextLine(position)) {
            val point = pointsRegistry.registerPoint()
            startNewLine(position, point.id)
            point.hitStatement
        } else {
            null
        }
    }

    fun isNextLine(position: Position): Boolean {
        val currentLineStart = currentLine?.lineNumber
        return if (currentLineStart == null) {
            true
        } else {
            currentLineStart != position.line
        }
    }

    fun startNewLine(position: Position, pointId: Int) {
        val current = currentLine
        if (current != null) {
            finishedLines += current
        }

        currentLine = LineInfo(position.line, position.column, pointId, mutableListOf())
        currentBranches.clear()
    }

    private fun InstrumentationResult.wrap(): IrExpression {
        val expression =
            asExpression ?: throw IllegalStateException("Instrumentation result can't be wrapped to expression; $leadingStatements")
        if (hitPoint == null && leadingStatements.isEmpty()) {
            return expression
        }
        return context.factory.call(context.builtIns.function0InvokeFun, expression.type) {
            arguments[0] = context.factory.lambda(expression.type, emptyList(), parentFunction) {
                addAll(allStatements)
            }
        }
    }

    private class InstrumentationResult private constructor(
        val leadingStatements: List<IrStatement>,
        val hitPoint: IrStatement?,
        val statement: IrStatement,
    ) {
        val asExpression: IrExpression
            get() {
                return statement as? IrExpression ?: error("Instrumented statement is not an expression, actual $statement")
            }
        val allStatements: List<IrStatement>
            get() {
                return if (hitPoint == null) {
                    leadingStatements + statement
                } else {
                    leadingStatements + hitPoint + statement
                }
            }
        val hasHitPoint: Boolean = hitPoint != null

        companion object {
            fun fromPointed(hitPointStatement: IrStatement?, instrumentedStatement: IrStatement): InstrumentationResult {
                return InstrumentationResult(emptyList(), hitPointStatement, instrumentedStatement)
            }

            fun fromPointed(
                leadingStatements: List<IrStatement?>,
                hitPointStatement: IrStatement?,
                statement: IrStatement,
            ): InstrumentationResult {
                return InstrumentationResult(leadingStatements.filterNotNull(), hitPointStatement, statement)
            }

            fun fromStatement(instrumentedStatement: IrStatement): InstrumentationResult {
                return InstrumentationResult(emptyList(), null, instrumentedStatement)
            }
        }
    }
}
