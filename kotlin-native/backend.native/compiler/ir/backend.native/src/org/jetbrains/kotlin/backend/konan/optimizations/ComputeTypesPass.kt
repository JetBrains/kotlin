/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isFinalClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.utils.copy
import org.jetbrains.kotlin.utils.forEachBit
import org.jetbrains.kotlin.utils.mapEachBit
import java.util.BitSet

internal class ComputeTypesPass(val context: Context) : BodyLoweringPass {
    private val unitType = context.irBuiltIns.unitType

    private fun IrClass.superClassesHierarchy(): List<IrClass> {
        val result = mutableListOf<IrClass>()
        var clazz = this
        while (!clazz.isAny()) {
            result.add(clazz)
            val superClass = clazz.superTypes.map { it.erasedUpperBound }.atMostOne { !it.isInterface }
                    ?: context.irBuiltIns.anyClass.owner
            clazz = superClass
        }
        result.add(clazz)

        result.reverse()
        return result
    }

    private fun leastCommonAncestor(types: List<IrType>): IrType? {
        if (types.isEmpty()) return null
        val isNullable = types.any { it.isNullable() }
        val classes = types.map { it.erasedUpperBound }
        // Since the analysis is local, nothing we can do about interfaces: if an interface is written to a variable,
        // we cannot replace its type with some class without knowing the whole types' hierarchy.
        if (classes.any { it.isInterface }) return null
        var commonAncestor = classes[0]
        var superClasses = commonAncestor.superClassesHierarchy()
        for (i in 1 until classes.size) {
            if (commonAncestor.isAny()) break
            val curClass = classes[i]
            val curSuperClasses = curClass.superClassesHierarchy()
            if (commonAncestor in curSuperClasses)
                continue
            if (curClass in superClasses) {
                commonAncestor = curClass
                superClasses = curSuperClasses
                continue
            }
            var idx = 0
            while (idx < superClasses.size && idx < curSuperClasses.size && superClasses[idx] == curSuperClasses[idx])
                ++idx
            commonAncestor = superClasses[idx - 1]
            superClasses = superClasses.take(idx)
        }

        return commonAncestor.defaultType.let { if (isNullable) it.makeNullable() else it }
    }

    private fun IrTypeOperatorCall.tryShortcutToArgument(): IrType? {
        if (this.operator != IrTypeOperator.IMPLICIT_CAST) return null
        val dstClass = this.typeOperand.erasedUpperBound
        if (dstClass.isInterface) return null
        if (!this.typeOperand.isNullable() && this.type.isNullable()) return null
        if (this.argument.type.erasedUpperBound.symbol.isSubtypeOfClass(dstClass.symbol))
            return (this.argument as? IrTypeOperatorCall)?.tryShortcutToArgument() ?: this.argument.type
        return null
    }

    private fun List<IrExpression>.computeType() = leastCommonAncestor(
            this.map { (it as? IrTypeOperatorCall)?.tryShortcutToArgument() ?: it.type }
                    .distinct()
                    .filterNot { it.isNothing() }
    )

    private fun IrElement.getImmediateChildren(): List<IrElement> {
        val result = mutableListOf<IrElement>()
        acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                result.add(element)
                // Do not recurse.
            }
        })
        return result
    }

    /*
     * The ultimate goal of this pass is to use DFA to compute more concrete types of some nodes.
     * Each visitXXX function in the visitor below takes variables values ~before~ the expression and returns
     * variables values ~after~ the expression. The variables values are represented as bit sets: one bit is reserved for
     * each variable write (including phi nodes). The pass is more or less straightforward except for two tricks:
     * one for loops and another for try/catch blocks.
     *
     * Let's start with try/catch blocks. In theory, an exception might be thrown anywhere inside the try clause and then
     * caught by one of the catch clauses. But maintaining precise CFG/phi node for it is kind of ridiculous (too many incoming edges),
     * so a simple approximation is used: save all possible variable writes inside the try clause and promote them to the catch clauses.
     *
     * As for the loops, the usual trick is to iterate the algorithm and merge the results of each iteration
     * until a stable point is reached. Here it's possible to do this merge only for IrGetValue nodes because only they use the
     * variables values being computed (other nodes also use them of course but indirectly).
     */

    private data class VariableWrite(val variable: IrElement, val value: IrExpression)

    private class ControlFlowMergePointInfo(val variable: IrElement) {
        val needValues = variable is IrExpression && !variable.type.erasedUpperBound.isFinalClass
        val variablesValues = BitSet()
        val variableWrites = if (needValues) BitSet() else null
    }

    // Some variables (catch block parameters and suspension point id parameters) are initialized by runtime.
    // Their usages look like uninitialized variable accesses. This is circumvented by putting some non-null value for their writes.
    private val externalWrites = BitSet()
    private val nothingValue = BitSet()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        context.log { "Analyzing ${container.render()}" }

        val allVariablesWrites = mutableListOf<VariableWrite>()
        val variableWriteMap = mutableMapOf<VariableWrite, Int>()
        val variableWrites = mutableMapOf<IrVariable, BitSet>()

        fun BitSet.computeType() = this.mapEachBit { allVariablesWrites[it].value }.computeType()

        irBody.accept(object : IrVisitor<BitSet, BitSet>() {
            fun getVariableWriteId(variable: IrElement, value: IrExpression) = VariableWrite(variable, value).let { write ->
                variableWriteMap.getOrPut(write) {
                    allVariablesWrites.add(write)
                    val index = allVariablesWrites.size - 1
                    (variable as? IrVariable)?.let {
                        variableWrites.getOrPut(it) { BitSet() }.set(index)
                    }
                    index
                }
            }

            private fun BitSet.format() = buildString {
                append('[')
                var first = true
                forEachBit {
                    if (!first) append(", ")
                    first = false
                    val (variable, value) = allVariablesWrites[it]
                    append((variable as? IrVariable)?.name ?: variable::class.java)
                    append(" = ")
                    append(value::class.java)
                }
                append(']')
            }

            val dummyUnitExpression = IrGetObjectValueImpl(
                    irBody.startOffset, irBody.endOffset, unitType, context.irBuiltIns.unitClass
            )

            // A simplification for handling try/catch blocks: this BitSet stores all the variable writes inside a try clause.
            // This allows for the corresponding catch clauses to see those writes (even when they are overwritten by control flow).
            var catchesVariablesValues: BitSet? = null
            val returnableBlockCFMPInfos = mutableMapOf<IrReturnableBlockSymbol, ControlFlowMergePointInfo>()
            val breaksCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
            val continuesCFMPInfos = mutableMapOf<IrLoop, ControlFlowMergePointInfo>()
            val getValueVariablesWrites = mutableMapOf<IrGetValue, BitSet>()
            val doWhileLoopForWhileLoops = mutableMapOf<IrWhileLoop, IrDoWhileLoop>()

            fun controlFlowMergePoint(cfmpInfo: ControlFlowMergePointInfo, value: IrExpression, variablesValues: BitSet): BitSet {
                val result = if (!cfmpInfo.needValues)
                    variablesValues
                else {
                    val id = getVariableWriteId(cfmpInfo.variable, value)
                    cfmpInfo.variableWrites!!.set(id)
                    variablesValues.copy().apply { set(id) }
                }

                cfmpInfo.variablesValues.or(result)
                return result
            }

            override fun visitElement(element: IrElement, data: BitSet): BitSet {
                var result = data
                for (node in element.getImmediateChildren())
                    result = node.accept(this, result)
                return result
            }

            override fun visitReturn(expression: IrReturn, data: BitSet): BitSet {
                val result = expression.value.accept(this, data)
                (expression.returnTargetSymbol as? IrReturnableBlockSymbol)?.let {
                    val cfmpInfo = returnableBlockCFMPInfos[it] ?: error("Unknown returnable block for ${expression.render()}")
                    controlFlowMergePoint(cfmpInfo, expression.value, result)
                }

                return nothingValue
            }

            override fun visitBlock(expression: IrBlock, data: BitSet): BitSet {
                val irReturnableBlock = expression as? IrReturnableBlock
                return if (irReturnableBlock == null) {
                    val result = visitElement(expression, data)
                    expression.type = (expression.statements.lastOrNull() as? IrExpression)?.type ?: expression.type
                    result
                } else {
                    val cfmpInfo = ControlFlowMergePointInfo(expression)
                    returnableBlockCFMPInfos[irReturnableBlock.symbol] = cfmpInfo
                    visitElement(expression, data)
                    returnableBlockCFMPInfos.remove(irReturnableBlock.symbol)
                    cfmpInfo.variableWrites?.computeType()?.let { expression.type = it }
                    cfmpInfo.variablesValues
                }
            }

            override fun visitWhen(expression: IrWhen, data: BitSet): BitSet {
                val cfmpInfo = ControlFlowMergePointInfo(expression)
                var result = data
                for (branch in expression.branches) {
                    result = branch.condition.accept(this, result)
                    val branchResult = branch.result.accept(this, result)
                    controlFlowMergePoint(cfmpInfo, branch.result, branchResult)
                }
                val isExhaustive = expression.branches.last().isUnconditional()
                if (isExhaustive) {
                    cfmpInfo.variableWrites?.computeType()?.let { expression.type = it }
                } else {
                    // A non-exhaustive when always has type Unit (or Nothing).
                    controlFlowMergePoint(cfmpInfo, dummyUnitExpression, result)
                }

                return cfmpInfo.variablesValues
            }

            override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: BitSet): BitSet {
                variableWrites[expression.suspensionPointIdParameter] = externalWrites
                val cfmpInfo = ControlFlowMergePointInfo(expression)
                val resultVV = expression.result.accept(this, data)
                controlFlowMergePoint(cfmpInfo, expression.result, resultVV)
                val resumeResultVV = expression.resumeResult.accept(this, data)
                controlFlowMergePoint(cfmpInfo, expression.resumeResult, resumeResultVV)

                return cfmpInfo.variablesValues
            }

            override fun visitTry(aTry: IrTry, data: BitSet): BitSet {
                val prevCatchesVV = catchesVariablesValues
                catchesVariablesValues = data.copy()
                val cfmpInfo = ControlFlowMergePointInfo(aTry)
                val tryVV = aTry.tryResult.accept(this, data)
                controlFlowMergePoint(cfmpInfo, aTry.tryResult, tryVV)
                val catchesVV = catchesVariablesValues!!
                catchesVariablesValues = prevCatchesVV
                for (aCatch in aTry.catches) {
                    variableWrites[aCatch.catchParameter] = externalWrites
                    val catchVV = aCatch.result.accept(this, catchesVV)
                    controlFlowMergePoint(cfmpInfo, aCatch.result, catchVV)
                }
                cfmpInfo.variableWrites?.computeType()?.let { aTry.type = it }

                return aTry.finallyExpression?.accept(this, cfmpInfo.variablesValues) ?: cfmpInfo.variablesValues
            }

            override fun visitBreak(jump: IrBreak, data: BitSet): BitSet {
                val cfmpInfo = breaksCFMPInfos[jump.loop] ?: error("Break from an unknown loop: ${jump.render()}")
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, data)

                return nothingValue
            }

            override fun visitContinue(jump: IrContinue, data: BitSet): BitSet {
                val cfmpInfo = continuesCFMPInfos[jump.loop] ?: error("Continue to an unknown loop: ${jump.render()}")
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, data)

                return nothingValue
            }

            fun handleDoWhileLoop(loop: IrLoop, variablesValues: BitSet): BitSet {
                var vvAtLoopStart = variablesValues

                context.log { "LOOP START: ${vvAtLoopStart.format()}" }

                var iter = 0
                while (true) {
                    ++iter
                    val prevVVAtLoopStart = vvAtLoopStart
                    val breaksCFMPInfo = ControlFlowMergePointInfo(loop)
                    val continuesCFMPInfo = ControlFlowMergePointInfo(loop)
                    breaksCFMPInfos[loop] = breaksCFMPInfo
                    continuesCFMPInfos[loop] = continuesCFMPInfo
                    val vvAtBodyEnd = loop.body?.accept(this, vvAtLoopStart) ?: vvAtLoopStart
                    val vvAtConditionStart =
                            controlFlowMergePoint(continuesCFMPInfo, dummyUnitExpression, vvAtBodyEnd)
                    val vvAtConditionEnd = loop.condition.accept(this, vvAtConditionStart)
                    vvAtLoopStart = vvAtConditionEnd
                    if (iter > 1) // Merge starting with the second iteration since the first is always executed.
                        vvAtLoopStart.or(prevVVAtLoopStart)

                    context.log { "LOOP ITER #$iter: ${vvAtLoopStart.format()}" }

                    if (vvAtLoopStart == prevVVAtLoopStart) {
                        breaksCFMPInfos.remove(loop)
                        continuesCFMPInfos.remove(loop)
                        return controlFlowMergePoint(breaksCFMPInfo, dummyUnitExpression, vvAtConditionEnd)
                    }
                }
            }

            override fun visitWhileLoop(loop: IrWhileLoop, data: BitSet): BitSet {
                // Replace
                //     while (condition) { .. }
                // with
                //     if (condition) { do { .. } while (condition) }
                val doWhileLoop = doWhileLoopForWhileLoops.getOrPut(loop) {
                    with(loop) { IrDoWhileLoopImpl(startOffset, endOffset, unitType, null) }
                }
                val cfmpInfo = ControlFlowMergePointInfo(doWhileLoop)
                val result = loop.condition.accept(this, data)
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, result)
                val loopResult = handleDoWhileLoop(loop, result)
                controlFlowMergePoint(cfmpInfo, dummyUnitExpression, loopResult)

                return cfmpInfo.variablesValues
            }

            override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BitSet) = handleDoWhileLoop(loop, data)

            override fun visitGetValue(expression: IrGetValue, data: BitSet): BitSet {
                val variable = expression.symbol.owner as? IrVariable ?: return data
                val variableWrites = variableWrites[variable]?.copy()?.apply { and(data) }
                        ?: error("A use of uninitialized variable ${variable.render()}")
                val mergedVariableWrites = getValueVariablesWrites.getOrPut(expression) { BitSet() }
                mergedVariableWrites.or(variableWrites)
                expression.type = mergedVariableWrites.computeType() ?: variable.type

                context.logMultiple {
                    +expression.render()
                    +"    ${mergedVariableWrites.format()}"
                }

                return data
            }

            fun setVariable(variable: IrVariable, value: IrExpression, variablesValues: BitSet): BitSet {
                val id = getVariableWriteId(variable, value)
                catchesVariablesValues?.set(id)
                val writes = variableWrites[variable] ?: error("A use of uninitialized variable ${variable.render()}")
                return variablesValues.copy().apply {
                    andNot(writes) // Forget all previous values.
                    set(id)
                }
            }

            override fun visitVariable(declaration: IrVariable, data: BitSet) =
                    declaration.initializer?.let {
                        val result = it.accept(this, data)
                        setVariable(declaration, it, result)
                    } ?: data

            override fun visitSetValue(expression: IrSetValue, data: BitSet) =
                    expression.value.accept(this, data).let {
                        setVariable(expression.symbol.owner as IrVariable, expression.value, it)
                    }
        }, data = BitSet())

        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitElement(element: IrElement): IrElement {
                element.transformChildrenVoid(this)

                return element
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                val values = variableWrites[declaration]?.mapEachBit { allVariablesWrites[it].value }
                val actualType = values?.computeType()
                if (actualType != null)
                    declaration.type = actualType

                return declaration
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val valueDeclaration = expression.symbol.owner
                return if (expression.type == valueDeclaration.type)
                    expression
                else {
                    val actualType = expression.type
                    expression.type = valueDeclaration.type
                    irBuilder.at(expression).irImplicitCast(expression, actualType)
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression.operator != IrTypeOperator.IMPLICIT_CAST) return expression
                val dstClass = expression.typeOperand.erasedUpperBound
                if (dstClass.isInterface) return expression
                if (!expression.typeOperand.isNullable() && expression.type.isNullable()) return expression
                if (expression.argument.type.erasedUpperBound.symbol.isSubtypeOfClass(dstClass.symbol))
                    return expression.argument
                return expression
            }
        })
    }
}
