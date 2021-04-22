/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.ir.actualCallee
import org.jetbrains.kotlin.backend.konan.ir.isOverridable
import org.jetbrains.kotlin.backend.konan.ir.isUnconditional
import org.jetbrains.kotlin.backend.konan.ir.isVirtualCall
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import java.util.*

/*
 * A data flow analysis to remove or move around calls to file initializers.
 * The goal is to find for each function and for each call site the set of
 * definitely initialized files before the corresponding call.
 *
 * This is done in three quite similar steps using global interprocedural analysis:
 * 1. For each function find the set of definitely initialized files after returning from the function.
 *    Handle all the functions in the reverse topological order.
 * 2. For each function find the set of definitely initialized files before executing the function's body.
 *    Handle all the functions in the topological order and use the results of the first step
 *    for updating the result after some function call.
 * 3. For each call site find the set of definitely initialized files before the actual call is made.
 *    Handle all the functions in the arbitrary order and use the results of the first step
 *    for updating the result. Then use the results from the second step to see if the initializer call
 *    could be extracted from the callee to the call site.
 *
 * All three steps use similar local intraprocedural data flow analysis on IR using an IR visitor
 * taking the set of already initialized files before evaluating some expression and returning the modified
 * set after evaluating that expression.
 */

internal object FileInitializersOptimization {
    private class AnalysisResult(val functionsRequiringInitializerCall: Set<IrFunction>,
                                 val callSitesRequiringInitializerCall: Set<IrFunctionAccessExpression>)

    private class InitializedFiles(val fileIds: Map<IrFile, Int>) {
        val beforeCall = mutableMapOf<IrFunction, BitSet>()
        val afterCall = mutableMapOf<IrFunction, BitSet>()
    }

    private class InterproceduralAnalysis(val context: Context, val callGraph: CallGraph,
                                          val rootSet: Set<IrFunction>) {
        fun analyze(): AnalysisResult {
            context.logMultiple {
                +"CALL GRAPH"
                callGraph.directEdges.forEach { (t, u) ->
                    +"    FUN $t"
                    u.callSites.forEach {
                        val label = when {
                            it.isVirtual -> "VIRTUAL"
                            callGraph.directEdges.containsKey(it.actualCallee) -> "LOCAL"
                            else -> "EXTERNAL"
                        }
                        +"        CALLS $label ${it.actualCallee}"
                    }
                    callGraph.reversedEdges[t]!!.forEach { +"        CALLED BY $it" }
                }
                +""
            }

            val condensation = DirectedGraphCondensationBuilder(callGraph).build()

            context.logMultiple {
                +"CONDENSATION"
                condensation.topologicalOrder.forEach { multiNode ->
                    +"    MULTI-NODE"
                    multiNode.nodes.forEach { +"        $it" }
                }
                +""
                +"CONDENSATION(DETAILED)"
                condensation.topologicalOrder.forEach { multiNode ->
                    +"    MULTI-NODE"
                    multiNode.nodes.forEach {
                        +"        $it"
                        callGraph.directEdges[it]!!.callSites
                                .filter { callGraph.directEdges.containsKey(it.actualCallee) }
                                .forEach { +"            CALLS ${it.actualCallee}" }
                        callGraph.reversedEdges[it]!!.forEach { +"            CALLED BY $it" }
                    }
                }
                +""
            }

            var fileId = 1
            val fileIds = mutableMapOf<IrFile, Int>()
            for (node in callGraph.directEdges.values) {
                val callerFile = node.symbol.irFile
                if (callerFile != null && fileIds[callerFile] == null)
                    fileIds[callerFile] = fileId++
                for (callSite in node.callSites) {
                    val calleeFile = callSite.actualCallee.irFile
                    if (calleeFile != null && fileIds[calleeFile] == null)
                        fileIds[calleeFile] = fileId++
                }
            }

            val initializedFiles = InitializedFiles(fileIds)

            context.log { "FIRST PHASE: compute initialized after call" }

            for (multiNode in condensation.topologicalOrder.reversed())
                analyze(multiNode, initializedFiles, AnalysisGoal.ComputeInitializedAfterCall)

            context.log { "SECOND PHASE: compute initialized before call" }

            // Each function from the root set can be called as the first one, so pessimistically assume that
            // none of the files has been initialized yet.
            for (node in callGraph.directEdges.values) {
                val function = node.symbol.irFunction ?: continue
                if (function in rootSet)
                    initializedFiles.beforeCall[function] = BitSet()
            }

            for (multiNode in condensation.topologicalOrder)
                analyze(multiNode, initializedFiles, AnalysisGoal.ComputeInitializedBeforeCall)

            context.log { "THIRD PHASE: collect call sites" }

            val callSitesRequiringInitializerCall = mutableSetOf<IrFunctionAccessExpression>()
            val callSitesNotRequiringInitializerCall = mutableSetOf<IrFunctionAccessExpression>()

            for (node in callGraph.directEdges.values) {
                intraproceduralAnalysis(node, initializedFiles, AnalysisGoal.CollectCallSites,
                        callSitesRequiringInitializerCall, callSitesNotRequiringInitializerCall)
            }

            val functionsWhoseInitializerCallCanBeExtractedToCallSites =
                    callSitesRequiringInitializerCall.map { it.actualCallee }
                            .toMutableSet().intersect(callSitesNotRequiringInitializerCall.map { it.actualCallee })

            val functionsRequiringInitializerCall = mutableSetOf<IrFunction>()
            initializedFiles.beforeCall.forEach { (function, functionInitializedFiles) ->
                val irFile = function.fileOrNull
                val backingField = (function as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.backingField
                val isDefaultAccessor = backingField != null && function.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                if (irFile == null ||
                        (!functionInitializedFiles.get(fileIds[irFile]!!)
                                && function !in functionsWhoseInitializerCallCanBeExtractedToCallSites
                                // Extract calls to file initializers off of default accessors to simplify their inlining.
                                && (!isDefaultAccessor || function in rootSet))
                ) {
                    functionsRequiringInitializerCall += function
                }
            }

            return AnalysisResult(functionsRequiringInitializerCall, callSitesRequiringInitializerCall)
        }

        private fun analyze(multiNode: DirectedGraphMultiNode<DataFlowIR.FunctionSymbol.Declared>,
                            initializedFiles: InitializedFiles,
                            analysisGoal: AnalysisGoal) {
            val nodes = multiNode.nodes.toList()

            context.logMultiple {
                +"Analyzing multiNode:\n    ${nodes.joinToString("\n   ") { it.toString() }}"
                nodes.forEach { from ->
                    +"IR"
                    +(from.irFunction?.dump() ?: "")
                    callGraph.directEdges[from]!!.callSites.forEach { to ->
                        +"CALL"
                        +"   from $from"
                        +"   to ${to.actualCallee}"
                    }
                }
            }

            if (nodes.size == 1)
                intraproceduralAnalysis(callGraph.directEdges[nodes[0]] ?: return, initializedFiles, analysisGoal)
            else {
                nodes.forEach { intraproceduralAnalysis(callGraph.directEdges[it]!!, initializedFiles, analysisGoal) }
                // The process is convergent since files can only be removed from the sets.
                var sum = nodes.sumBy {
                    (initializedFiles.beforeCall[it.irFunction!!]?.cardinality() ?: 0) + (initializedFiles.afterCall[it.irFunction]?.cardinality() ?: 0)
                }
                do {
                    val prevSum = sum
                    nodes.forEach { intraproceduralAnalysis(callGraph.directEdges[it]!!, initializedFiles, analysisGoal) }
                    sum = nodes.sumBy {
                        (initializedFiles.beforeCall[it.irFunction!!]?.cardinality() ?: 0) + (initializedFiles.afterCall[it.irFunction]?.cardinality() ?: 0)
                    }
                } while (sum != prevSum)
            }
        }

        private val executeImplSymbol = context.ir.symbols.executeImpl
        private val getContinuationSymbol = context.ir.symbols.getContinuation

        private var dummySet = mutableSetOf<IrFunctionAccessExpression>()

        private enum class AnalysisGoal {
            ComputeInitializedAfterCall,
            ComputeInitializedBeforeCall,
            CollectCallSites
        }

        private fun intraproceduralAnalysis(
                node: CallGraphNode,
                initializedFiles: InitializedFiles,
                analysisGoal: AnalysisGoal,
                callSitesRequiringInitializerCall: MutableSet<IrFunctionAccessExpression> = dummySet,
                callSitesNotRequiringInitializerCall: MutableSet<IrFunctionAccessExpression> = dummySet
        ) {
            val caller = node.symbol.irFunction ?: return
            val body = caller.body ?: return
            val initializedFilesBeforeCall = BitSet()
            initializedFiles.beforeCall[caller]?.let { initializedFilesBeforeCall.or(it) }
            // Since the function has been called, the file is bound to be initialized.
            initializedFilesBeforeCall.set(initializedFiles.fileIds[caller.file]!!)

            val producerInvocations = mutableMapOf<IrExpression, IrCall>()
            val jobInvocations = mutableMapOf<IrCall, IrCall>()
            val virtualCallSites = mutableMapOf<IrCall, MutableList<CallGraphNode.CallSite>>()
            for (callSite in node.callSites) {
                val call = callSite.call
                val irCall = call.irCallSite as? IrCall ?: continue
                if (irCall.origin == STATEMENT_ORIGIN_PRODUCER_INVOCATION)
                    producerInvocations[irCall.dispatchReceiver!!] = irCall
                else if (irCall.origin == STATEMENT_ORIGIN_JOB_INVOCATION)
                    jobInvocations[irCall.getValueArgument(0) as IrCall] = irCall
                if (call !is DataFlowIR.Node.VirtualCall) continue
                virtualCallSites.getOrPut(irCall) { mutableListOf() }.add(callSite)
            }
            val returnTargetsInitializedFiles = mutableMapOf<IrReturnTargetSymbol, BitSet>()
            val initializedFilesAtLoopsBreaks = mutableMapOf<IrLoop, BitSet>()
            val initializedFilesAtLoopsContinues = mutableMapOf<IrLoop, BitSet>()
            val builder = context.createIrBuilder(caller.symbol)
            val block = builder.irBlock { body.statements.forEach { +it } }
            block.accept(object : IrElementVisitor<BitSet, BitSet> {
                private fun intersectInitializedFiles(previous: BitSet?, current: BitSet) =
                        previous?.copy()?.also { it.and(current) } ?: current

                private fun <K> intersectInitializedFiles(map: MutableMap<K, BitSet>, key: K, set: BitSet) {
                    val previous = map[key]
                    if (previous == null)
                        map[key] = set.copy()
                    else
                        previous.and(set)
                }

                override fun visitElement(element: IrElement, data: BitSet): BitSet = TODO(element.render())
                override fun visitExpression(expression: IrExpression, data: BitSet): BitSet = TODO(expression.render())
                override fun visitDeclaration(declaration: IrDeclarationBase, data: BitSet): BitSet = TODO(declaration.render())

                override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BitSet) = expression.argument.accept(this, data)
                override fun <T> visitConst(expression: IrConst<T>, data: BitSet) = data
                override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BitSet) = data

                override fun visitGetValue(expression: IrGetValue, data: BitSet) = data
                override fun visitSetValue(expression: IrSetValue, data: BitSet) = expression.value.accept(this, data)
                override fun visitVariable(declaration: IrVariable, data: BitSet) = declaration.initializer?.accept(this, data) ?: data

                override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: BitSet) = expression.result.accept(this, data)
                override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: BitSet) = expression.result.accept(this, data)

                override fun visitGetField(expression: IrGetField, data: BitSet) = expression.receiver?.accept(this, data) ?: data
                override fun visitSetField(expression: IrSetField, data: BitSet) =
                        expression.value.accept(this, expression.receiver?.accept(this, data) ?: data)

                override fun visitFunctionReference(expression: IrFunctionReference, data: BitSet) = data
                override fun visitVararg(expression: IrVararg, data: BitSet) = data

                override fun visitBreak(jump: IrBreak, data: BitSet): BitSet {
                    intersectInitializedFiles(initializedFilesAtLoopsBreaks, jump.loop, data)
                    return data
                }
                override fun visitContinue(jump: IrContinue, data: BitSet): BitSet {
                    intersectInitializedFiles(initializedFilesAtLoopsContinues, jump.loop, data)
                    return data
                }
                // A while loop might not execute even a single iteration.
                override fun visitWhileLoop(loop: IrWhileLoop, data: BitSet) =
                        loop.condition.accept(this, data).also { loop.body?.accept(this, it) }
                override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BitSet): BitSet {
                    val bodyFallThroughResult = loop.body?.accept(this, data) ?: data
                    val continuesResult = initializedFilesAtLoopsContinues[loop]
                    // We can end up in the condition part either by falling through the entire body or by executing one of the continue clauses.
                    val bodyResult = intersectInitializedFiles(continuesResult, bodyFallThroughResult)
                    val conditionResult = loop.condition.accept(this, bodyResult)
                    val breaksResult = initializedFilesAtLoopsBreaks[loop]
                    // A loop can be finished either by checking the condition or by executing a break clause.
                    return intersectInitializedFiles(breaksResult, conditionResult)
                }

                private fun updateResultForReturnTarget(symbol: IrReturnTargetSymbol, set: BitSet) =
                        intersectInitializedFiles(returnTargetsInitializedFiles, symbol, set)

                override fun visitReturn(expression: IrReturn, data: BitSet) =
                        expression.value.accept(this, data).also {
                            updateResultForReturnTarget(expression.returnTargetSymbol, it)
                        }

                override fun visitContainerExpression(expression: IrContainerExpression, data: BitSet): BitSet {
                    var set = data
                    expression.statements.forEach { set = it.accept(this, set) }
                    return if (expression !is IrReturnableBlock)
                        set
                    else {
                        updateResultForReturnTarget(expression.symbol, set)
                        returnTargetsInitializedFiles[expression.symbol]!!
                    }
                }

                override fun visitWhen(expression: IrWhen, data: BitSet): BitSet {
                    val firstBranch = expression.branches.first()
                    val firstConditionResult = firstBranch.condition.accept(this, data)
                    val bodiesResult = firstBranch.result.accept(this, firstConditionResult)
                    var conditionsResult = firstConditionResult
                    for (i in 1 until expression.branches.size) {
                        val branch = expression.branches[i]
                        conditionsResult = branch.condition.accept(this, conditionsResult)
                        val branchResult = branch.result.accept(this, conditionsResult)
                        bodiesResult.and(branchResult)
                    }
                    val isExhaustive = expression.branches.last().isUnconditional()
                    return if (isExhaustive) {
                        // One of the branches must have been executed.
                        bodiesResult
                    } else {
                        // The first condition is always executed.
                        firstConditionResult
                    }
                }

                override fun visitThrow(expression: IrThrow, data: BitSet): BitSet {
                    expression.value.accept(this, data)
                    return data // Conservative but correct.
                }

                override fun visitTry(aTry: IrTry, data: BitSet): BitSet {
                    require(aTry.finallyExpression == null)
                    aTry.tryResult.accept(this, data)
                    // Catch blocks can't assume that the try part has been executed entirely,
                    // so only take what was known at the beginning of the try block.
                    aTry.catches.forEach { it.result.accept(this, data) }
                    // Since the try part could have been executed with an exception which then could've been caught by
                    // some of the catch clauses, it is incorrect to take the try block's result,
                    // so conservatively don't change the result.
                    return data
                }

                private fun getResultAfterCall(function: IrFunction, set: BitSet): BitSet {
                    val result = initializedFiles.afterCall[function]
                    if (result == null) {
                        val file = function.fileOrNull ?: return set
                        val fileId = initializedFiles.fileIds[file]!!
                        return if (set.get(fileId))
                            set
                        else
                            set.copy().also { it.set(fileId) }
                    }
                    return result.copy().also { it.or(set) }
                }

                private fun updateResultForFunction(function: IrFunction, set: BitSet) {
                    if (analysisGoal != AnalysisGoal.ComputeInitializedBeforeCall) return
                    intersectInitializedFiles(initializedFiles.beforeCall, function, set)
                }

                override fun visitGetObjectValue(expression: IrGetObjectValue, data: BitSet): BitSet {
                    val objectClass = expression.symbol.owner
                    val constructor = objectClass.constructors.toList().atMostOne()
                    if (constructor != null) {
                        updateResultForFunction(constructor, data)
                    } else {
                        require(objectClass.isExternal || objectClass is IrLazyClass)
                    }
                    val file = objectClass.fileOrNull ?: return data
                    val fileId = initializedFiles.fileIds[file]!!
                    if (data.get(fileId)) return data
                    return data.copy().also { it.set(fileId) }
                }

                private fun processCall(expression: IrFunctionAccessExpression, actualCallee: IrFunction, data: BitSet): BitSet {
                    val arguments = expression.getArgumentsWithIr()
                    var curData = data
                    arguments.forEach { curData = it.second.accept(this, curData) }
                    updateResultForFunction(actualCallee, curData)
                    val file = actualCallee.fileOrNull
                    val fileId = file?.let { initializedFiles.fileIds[it]!! } ?: 0
                    if (analysisGoal == AnalysisGoal.CollectCallSites && file != null
                            && !actualCallee.isOverridable // Only extract initializer calls from non-virtual functions.
                            && !initializedFiles.beforeCall[actualCallee]!!.get(fileId)) {
                        if (curData.get(fileId))
                            callSitesNotRequiringInitializerCall += expression
                        else
                            callSitesRequiringInitializerCall += expression
                    }
                    return getResultAfterCall(actualCallee, curData)
                }

                private fun processExecuteImpl(expression: IrCall, data: BitSet): BitSet {
                    var curData = processCall(expression, expression.symbol.owner, data)
                    val producerInvocation = producerInvocations[expression.getValueArgument(2)!!]!!
                    // Producer is invoked right here in the same thread, so can update the result.
                    // Albeit this call site is a fictitious one, it is always a virtual one, which aren't optimized for now.
                    curData = visitCall(producerInvocation, curData)
                    val jobInvocation = jobInvocations[producerInvocation]!!
                    if (analysisGoal != AnalysisGoal.CollectCallSites) // A fictitious call site - skip it explicitly since it's always non-virtual.
                        visitCall(jobInvocation, curData)
                    // Actual job could be invoked on another thread, thus can't take the result from that call.
                    return curData
                }

                override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: BitSet) =
                        processCall(expression, expression.actualCallee, data)

                override fun visitCall(expression: IrCall, data: BitSet): BitSet {
                    if (expression.symbol == executeImplSymbol)
                        return processExecuteImpl(expression, data)
                    if (expression.symbol == getContinuationSymbol)
                        return data
                    if (!expression.isVirtualCall)
                        return processCall(expression, expression.actualCallee, data)
                    val devirtualizedCallSite = virtualCallSites[expression] ?: return data
                    val arguments = expression.getArgumentsWithIr()
                    var curData = data
                    arguments.forEach { curData = it.second.accept(this, curData) }
                    var set = BitSet()
                    var first = true
                    for (callSite in devirtualizedCallSite) {
                        val callee = callSite.actualCallee.irFunction ?: error("No IR for: ${callSite.actualCallee}")
                        updateResultForFunction(callee, curData)
                        if (first) {
                            set = getResultAfterCall(callee, BitSet())
                            first = false
                        } else {
                            val otherSet = getResultAfterCall(callee, BitSet())
                            set.and(otherSet)
                        }
                    }
                    return curData.copy().also { it.or(set) }
                }
            }, initializedFilesBeforeCall)

            if (analysisGoal == AnalysisGoal.ComputeInitializedAfterCall)
                initializedFiles.afterCall[caller] = returnTargetsInitializedFiles[caller.symbol] ?: initializedFilesBeforeCall
        }
    }

    fun removeRedundantCalls(context: Context, callGraph: CallGraph, rootSet: Set<IrFunction>) {
        val analysisResult = InterproceduralAnalysis(context, callGraph, rootSet).analyze()

        var numberOfFunctions = 0
        var numberOfRemovedInitializerCalls = 0

        context.irModule!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression !in analysisResult.callSitesRequiringInitializerCall) return expression
                val callee = expression.actualCallee
                if (callee in analysisResult.functionsRequiringInitializerCall) return expression
                val body = callee.body ?: return expression
                val initializerCalls = (body as IrBlockBody).statements.filter {
                    val calleeOrigin = (it as? IrCall)?.symbol?.owner?.origin
                    calleeOrigin == DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER
                            || calleeOrigin == DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
                            || calleeOrigin == DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER
                }
                if (initializerCalls.isEmpty()) return expression

                return IrBlockImpl(
                        expression.startOffset, expression.endOffset,
                        expression.type
                ).apply {
                    statements.addAll(initializerCalls.map {
                        val initializer = (it as IrCall).symbol
                        IrCallImpl(
                                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                                context.irBuiltIns.unitType,
                                initializer,
                                typeArgumentsCount = 0, valueArgumentsCount = 0
                        )
                    })
                    statements.add(expression)
                }
            }
        })

        context.irModule!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                ++numberOfFunctions
                if (declaration in analysisResult.functionsRequiringInitializerCall) return declaration
                ++numberOfRemovedInitializerCalls
                val body = declaration.body ?: return declaration
                (body as IrBlockBody).statements.removeAll {
                    val calleeOrigin = (it as? IrCall)?.symbol?.owner?.origin
                    calleeOrigin == DECLARATION_ORIGIN_FILE_GLOBAL_INITIALIZER
                            || calleeOrigin == DECLARATION_ORIGIN_FILE_THREAD_LOCAL_INITIALIZER
                            || calleeOrigin == DECLARATION_ORIGIN_FILE_STANDALONE_THREAD_LOCAL_INITIALIZER
                }
                return declaration
            }
        })

        context.log { "Removed ${numberOfRemovedInitializerCalls * 100.0 / numberOfFunctions}% initializers" }
    }
}