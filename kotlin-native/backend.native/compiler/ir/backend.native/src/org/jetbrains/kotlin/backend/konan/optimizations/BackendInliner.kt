/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.isVirtualCall
import org.jetbrains.kotlin.backend.konan.lower.erasure
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class BackendInliner(
        val generationState: NativeGenerationState,
        val moduleDFG: ModuleDFG,
        val devirtualizedCallSites: Map<DataFlowIR.Node.VirtualCall, DevirtualizationAnalysis.DevirtualizedCallSite>,
        val callGraph: CallGraph,
) {
    private val context = generationState.context
    private val symbols = context.ir.symbols
    private val invokeSuspendFunction = symbols.invokeSuspendFunction
    private val rootSet = callGraph.rootSet

    private inline fun DataFlowIR.FunctionBody.forEachVirtualCall(block: (DataFlowIR.Node.VirtualCall) -> Unit) =
            forEachNonScopeNode { node ->
                if (node is DataFlowIR.Node.VirtualCall)
                    block(node)
            }

    fun run(): Map<DataFlowIR.Node.VirtualCall, DevirtualizationAnalysis.DevirtualizedCallSite> {
        val rebuiltDevirtualizedCallSites = mutableMapOf<DataFlowIR.Node.VirtualCall, DevirtualizationAnalysis.DevirtualizedCallSite>()
        val computationStates = mutableMapOf<DataFlowIR.FunctionSymbol.Declared, ComputationState>()
        val stack = rootSet.toMutableList()
        for (root in stack)
            computationStates[root] = ComputationState.NEW
        while (stack.isNotEmpty()) {
            val functionSymbol = stack.peek()!!
            val function = moduleDFG.functions[functionSymbol]!!
            val state = computationStates[functionSymbol]!!
            val callSites = callGraph.directEdges[functionSymbol]!!.callSites.filter {
                !it.isVirtual && callGraph.directEdges.containsKey(it.actualCallee)
            }
            when (state) {
                ComputationState.NEW -> {
                    computationStates[functionSymbol] = ComputationState.PENDING
                    for (callSite in callSites) {
                        val calleeSymbol = callSite.actualCallee as DataFlowIR.FunctionSymbol.Declared
                        if (computationStates[calleeSymbol] == null || computationStates[calleeSymbol] == ComputationState.NEW) {
                            computationStates[calleeSymbol] = ComputationState.NEW
                            stack.push(calleeSymbol)
                        }
//                        when (computationStates[calleeSymbol]) {
//                            null -> {
//                                computationStates[calleeSymbol] = ComputationState.NEW
//                                stack.push(calleeSymbol)
//                            }
//                            ComputationState.NEW, ComputationState.DONE -> Unit
//                            ComputationState.PENDING -> TODO()
//                        }
                    }
                }

                ComputationState.PENDING -> {
                    stack.pop()
                    computationStates[functionSymbol] = ComputationState.DONE

                    val irFunction = functionSymbol.irFunction ?: continue
                    val irBody = irFunction.body ?: continue
////                    if (irFunction.name.asString() == "foo")
//                    println("Handling ${irFunction.render()}")
                    val functionsToInline = mutableSetOf<IrFunction>()
                    val devirtualizedCallSitesFromFunctionsToInline = mutableMapOf<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>()
                    for (callSite in callSites) {
                        val calleeSymbol = callSite.actualCallee as DataFlowIR.FunctionSymbol.Declared
////                        if (irFunction.name.asString() == "foo") {
//                        println("    call to ${calleeSymbol.irFunction?.render()}")
////                            println("        ${computationStates[calleeSymbol]} ${calleeSymbol.irFunction?.render()}")
////                        }
                        if (computationStates[calleeSymbol] != ComputationState.DONE) continue
                        val calleeIrFunction = calleeSymbol.irFunction ?: continue
                        val callee = moduleDFG.functions[calleeSymbol]!!
                        val calleeSize = callee.body.allScopes.sumOf { it.nodes.size }
                        var isALoop = false
                        callee.body.forEachNonScopeNode { node ->
                            if (node is DataFlowIR.Node.Call && node.callee == calleeSymbol)
                                isALoop = true
                            if (node is DataFlowIR.Node.VirtualCall) {
                                val devirtualizedCallSite = devirtualizedCallSites[node]
                                val maxUnfoldFactor = if (node is DataFlowIR.Node.ItableCall)
                                    DevirtualizationUnfoldFactors.IR_DEVIRTUALIZED_ITABLE_CALL else DevirtualizationUnfoldFactors.IR_DEVIRTUALIZED_VTABLE_CALL
                                if (devirtualizedCallSite != null) {
                                    val possibleCallees = devirtualizedCallSite.possibleCallees.groupBy {
                                        it.callee as? DataFlowIR.FunctionSymbol.Declared ?: return@forEachNonScopeNode
                                    }
                                    if (possibleCallees.size <= maxUnfoldFactor && devirtualizedCallSite.possibleCallees.any { it.callee == calleeSymbol })
                                        isALoop = true
                                }
                            }
                        }
//                        //if (irFunction.name.asString() == "foo")
//                        println("        $isALoop $calleeSize")
                        if (!isALoop && calleeSize <= 25 // TODO: To a function. Also use relative criterion along with the absolute one.
                                && calleeIrFunction is IrSimpleFunction // TODO: Support constructors.
                                && !calleeIrFunction.overrides(invokeSuspendFunction.owner) // TODO: Is it worth trying to support?
                                /*&& irFunction.fileOrNull?.path?.endsWith("z.kt") == true*/) {
                            if (functionsToInline.add(calleeIrFunction)) {
                                callee.body.forEachVirtualCall { node ->
                                    val devirtualizedCallSite = devirtualizedCallSites[node]
                                    if (devirtualizedCallSite != null)
                                        devirtualizedCallSitesFromFunctionsToInline[node.irCallSite!!] = devirtualizedCallSite
                                }
                            }
                        }
                    }

                    if (functionsToInline.isEmpty()) {
//                        println("Nothing to inline to ${irFunction.render()}")
                        function.body.forEachVirtualCall { node ->
                            val devirtualizedCallSite = devirtualizedCallSites[node]
                            if (devirtualizedCallSite != null)
                                rebuiltDevirtualizedCallSites[node] = devirtualizedCallSite
                        }
                    } else {
//                        println("Preparing to inline to ${irFunction.render()}")
////                        functionsToInline.forEach { println("    ${it.dump()}") }
//                        functionsToInline.forEach { println("    ${it.render()}") }
//                        println("BEFORE: ${irFunction.dump()}")
                        val inliner = FunctionInlining(
                                context,
                                inlineFunctionResolver = object : InlineFunctionResolver() {
                                    override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol) =
                                            symbol.owner !in functionsToInline
                                },
                                devirtualizedCallSitesFromFunctionsToInline,
                        )
                        val devirtualizedCallSitesFromInlinedFunctions = inliner.lower(irBody, irFunction)

//                        println("AFTER: ${irFunction.dump()}")

                        LivenessAnalysis.run(irBody) { it is IrSuspensionPoint }
                                .forEach { (irElement, liveVariables) ->
                                    generationState.liveVariablesAtSuspensionPoints[irElement as IrSuspensionPoint] = liveVariables
                                }

                        val devirtualizedCallSitesFromFunction = mutableMapOf<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>()
                        function.body.forEachVirtualCall { node ->
                            val devirtualizedCallSite = devirtualizedCallSites[node]
                            if (devirtualizedCallSite != null)
                                devirtualizedCallSitesFromFunction[node.irCallSite!!] = devirtualizedCallSite
                        }

                        val rebuiltFunction = FunctionDFGBuilder(generationState, moduleDFG.symbolTable).build(irFunction, irBody)
                        moduleDFG.functions[functionSymbol] = rebuiltFunction
                        rebuiltFunction.body.forEachVirtualCall { node ->
                            val irCallSite = node.irCallSite!!
                            val devirtualizedCallSite = devirtualizedCallSitesFromInlinedFunctions[irCallSite]
                                    ?: devirtualizedCallSitesFromFunction[irCallSite]
                            if (devirtualizedCallSite != null)
                                rebuiltDevirtualizedCallSites[node] = devirtualizedCallSite
                        }

                        /*
                        +                    val body = when (declaration) {
                        +                        is IrFunction -> {
                        +                            context.logMultiple {
                        +                                +"Analysing function ${declaration.render()}"
                        +                                +"IR: ${declaration.dump()}"
                        +                            }
                        +                            declaration.body!!.also { body ->
                        +                                LivenessAnalysis.run(body) { it is IrSuspensionPoint }
                        +                                        .forEach { (irElement, liveVariables) ->
                        +                                            generationState.liveVariablesAtSuspensionPoints[irElement as IrSuspensionPoint] = liveVariables
                        +                                        }
                        +                            }
                        +                        }
                        +
                        +                        is IrField -> {
                        +                            context.logMultiple {
                        +                                +"Analysing global field ${declaration.render()}"
                        +                                +"IR: ${declaration.dump()}"
                        +                            }
                        +                            val initializer = declaration.initializer!!
                        +                            IrSetFieldImpl(initializer.startOffset, initializer.endOffset, declaration.symbol, null,
                        +                                    initializer.expression, context.irBuiltIns.unitType)
                        +                        }
                        +
                        +                        else -> error("Unexpected declaration: ${declaration.render()}")
                        +                    }
                        +
                        +
                        +//                    println("AFTER polishing: ${declaration.dump()}")
                        +                    val function = FunctionDFGBuilder(generationState, input.moduleDFG.symbolTable).build(declaration, body)
                        +                    input.moduleDFG.functions[function.symbol] = function
                         */
                    }
                }

                ComputationState.DONE -> {
                    stack.pop()
                }
            }
        }

        return rebuiltDevirtualizedCallSites
    }

    private enum class ComputationState {
        NEW,
        PENDING,
        DONE
    }

}

internal abstract class InlineFunctionResolver {
    fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (shouldExcludeFunctionFromInlining(symbol)) return null

        val owner = symbol.owner
        return (owner as? IrSimpleFunction)?.resolveFakeOverride() ?: owner
    }

    protected abstract fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean
}

internal class FunctionInlining(
        private val context: Context,
        private val inlineFunctionResolver: InlineFunctionResolver,
        private val devirtualizedCallSites: Map<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>,
) : IrElementTransformerVoidWithContext() {
    private var containerScope: ScopeWithIr? = null
    private val elementsWithLocationToPatch = hashSetOf<IrGetValue>()
    private val copiedDevirtualizedCallSites = mutableMapOf<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>()

    fun lower(irBody: IrBody, container: IrDeclaration): Map<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite> {
        // TODO container: IrSymbolDeclaration
        containerScope = createScope(container as IrSymbolOwner)
        irBody.accept(this, null)
        containerScope = null

        irBody.patchDeclarationParents(container as? IrDeclarationParent ?: container.parent)

        return copiedDevirtualizedCallSites
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid(this)
        val calleeSymbol = when (expression) {
            is IrCall -> expression.symbol
            is IrConstructorCall -> expression.symbol
            else -> return expression
        }

        val actualCallee = inlineFunctionResolver.getFunctionDeclaration(calleeSymbol)
        if (actualCallee?.body == null || expression.isVirtualCall) {
            return expression
        }

        val parent = allScopes.map { it.irElement }.filterIsInstance<IrDeclarationParent>().lastOrNull()
                ?: allScopes.map { it.irElement }.filterIsInstance<IrDeclaration>().lastOrNull()?.parent
                ?: containerScope?.irElement as? IrDeclarationParent
                ?: (containerScope?.irElement as? IrDeclaration)?.parent

        val inliner = Inliner(expression, actualCallee, currentScope ?: containerScope!!, parent, context)
        return inliner.inline()
    }

    private inner class Inliner(
            val callSite: IrFunctionAccessExpression,
            val callee: IrFunction,
            val currentScope: ScopeWithIr,
            val parent: IrDeclarationParent?,
            val context: CommonBackendContext
    ) {
        val copyIrElement = run {
//            val typeParameters =
//                    if (callee is IrConstructor)
//                        callee.parentAsClass.typeParameters
//                    else callee.typeParameters
//            val typeArguments =
//                    (0 until callSite.typeArgumentsCount).associate {
//                        typeParameters[it].symbol to callSite.getTypeArgument(it)
//                    }
            DeepCopyIrTreeWithSymbolsForInliner(/*typeArguments*/null, parent, devirtualizedCallSites, copiedDevirtualizedCallSites)
        }

        val substituteMap = mutableMapOf<IrValueParameter, IrExpression>()

        fun inline() = inlineFunction(callSite, callee, callee/*.originalFunction*/)

        private fun <E : IrElement> E.copy(): E {
            @Suppress("UNCHECKED_CAST")
            return copyIrElement.copy(this) as E
        }

        private fun inlineFunction(
                callSite: IrFunctionAccessExpression,
                callee: IrFunction,
                originalInlinedElement: IrElement,
        ): IrReturnableBlock {
            val copiedCallee = callee.copy().apply {
                parent = callee.parent
            }
//            println("AFTER copying: ${copiedCallee.dump()}")

            val evaluationStatements = evaluateArguments(callSite, copiedCallee)
            val statements = (copiedCallee.body as? IrBlockBody)?.statements
                    ?: error("Body not found for function ${callee.render()}")

            val irReturnableBlockSymbol = IrReturnableBlockSymbolImpl()
            val endOffset = statements.lastOrNull()?.endOffset ?: callee.endOffset
            /* creates irBuilder appending to the end of the given returnable block: thus why we initialize
             * irBuilder with (..., endOffset, endOffset).
             */
            val irBuilder = context.createIrBuilder(irReturnableBlockSymbol, endOffset, endOffset)

            val transformer = ParameterSubstitutor()
            val newStatements = statements.map { it.transform(transformer, data = null) as IrStatement }

            val returnType = callee.returnType.erasure()

            val inlinedBlock = IrInlinedFunctionBlockImpl(
                    startOffset = callSite.startOffset,
                    endOffset = callSite.endOffset,
                    type = returnType,
                    inlineCall = callSite,
                    inlinedElement = originalInlinedElement,
                    origin = null,
                    statements = evaluationStatements + newStatements
            )

            // Note: here we wrap `IrInlinedFunctionBlock` inside `IrReturnableBlock` because such way it is easier to
            // control special composite blocks that are inside `IrInlinedFunctionBlock`
            return IrReturnableBlockImpl(
                    startOffset = callSite.startOffset,
                    endOffset = callSite.endOffset,
                    type = returnType,
                    symbol = irReturnableBlockSymbol,
                    origin = null,
                    statements = listOf(inlinedBlock),
            ).apply {
                transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid(this)

                        if (expression.returnTargetSymbol == copiedCallee.symbol)
                            return irBuilder.at(expression).irReturn(expression.value)

                        return expression
                    }
                })
                patchDeclarationParents(parent) // TODO: Why it is not enough to just run SetDeclarationsParentVisitor?
            }
        }

        private inner class ParameterSubstitutor : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val newExpression = super.visitGetValue(expression) as IrGetValue
                val argument = substituteMap[newExpression.symbol.owner] ?: return newExpression

                argument.transformChildrenVoid(this) // Default argument can contain subjects for substitution.

                return if (argument is IrGetValue && argument in elementsWithLocationToPatch)
                    argument.copyWithOffsets(newExpression.startOffset, newExpression.endOffset)
                else
                    argument.copy()
            }

            override fun visitElement(element: IrElement) = element.accept(this, null)
        }

        private inner class ParameterToArgument(
                val parameter: IrValueParameter,
                val argumentExpression: IrExpression,
                val isDefaultArg: Boolean = false
        ) {
            val isImmutableVariableLoad: Boolean
                get() = argumentExpression.let { argument ->
                    argument is IrGetValue && !argument.symbol.owner.let { it is IrVariable && it.isVar }
                }
        }

        // callee might be a copied version of callsite.symbol.owner
        private fun buildParameterToArgument(callSite: IrFunctionAccessExpression, callee: IrFunction): List<ParameterToArgument> {
            val parameterToArgument = mutableListOf<ParameterToArgument>()

            if (callSite.dispatchReceiver != null && callee.dispatchReceiverParameter != null)
                parameterToArgument += ParameterToArgument(
                        parameter = callee.dispatchReceiverParameter!!,
                        argumentExpression = callSite.dispatchReceiver!!
                )

            val valueArguments =
                    callSite.symbol.owner.valueParameters.map { callSite.getValueArgument(it.index) }.toMutableList()

            if (callee.extensionReceiverParameter != null) {
                parameterToArgument += ParameterToArgument(
                        parameter = callee.extensionReceiverParameter!!,
                        argumentExpression = if (callSite.extensionReceiver != null) {
                            callSite.extensionReceiver!!
                        } else {
                            // Special case: lambda with receiver is called as usual lambda:
                            valueArguments.removeAt(0)!!
                        }
                )
            } else if (callSite.extensionReceiver != null) {
                // Special case: usual lambda is called as lambda with receiver:
                valueArguments.add(0, callSite.extensionReceiver!!)
            }

            val parametersWithDefaultToArgument = mutableListOf<ParameterToArgument>()
            for (parameter in callee.valueParameters) {
                val argument = valueArguments[parameter.index]
                when {
                    argument != null -> {
                        parameterToArgument += ParameterToArgument(
                                parameter = parameter,
                                argumentExpression = argument
                        )
                    }

                    // After ExpectDeclarationsRemoving pass default values from expect declarations
                    // are represented correctly in IR.
                    parameter.defaultValue != null -> {  // There is no argument - try default value.
                        parametersWithDefaultToArgument += ParameterToArgument(
                                parameter = parameter,
                                argumentExpression = parameter.defaultValue!!.expression,
                                isDefaultArg = true
                        )
                    }

                    parameter.varargElementType != null -> {
                        val emptyArray = IrVarargImpl(
                                startOffset = callSite.startOffset,
                                endOffset = callSite.endOffset,
                                type = parameter.type,
                                varargElementType = parameter.varargElementType!!
                        )
                        parameterToArgument += ParameterToArgument(
                                parameter = parameter,
                                argumentExpression = emptyArray
                        )
                    }

                    else -> {
                        val message = "Incomplete expression: call to ${callee.render()} " +
                                "has no argument at index ${parameter.index}"
                        throw Error(message)
                    }
                }
            }
            // All arguments except default are evaluated at callsite,
            // but default arguments are evaluated inside callee.
            return parameterToArgument + parametersWithDefaultToArgument
        }

        private fun evaluateArguments(callSite: IrFunctionAccessExpression, callee: IrFunction): List<IrStatement> {
            val arguments = buildParameterToArgument(callSite, callee)
            val evaluationStatements = mutableListOf<IrVariable>()
            val evaluationStatementsFromDefault = mutableListOf<IrVariable>()
            val substitutor = ParameterSubstitutor()
            arguments.forEach { argument ->
                val parameter = argument.parameter

                // Arguments may reference the previous ones - substitute them.
                val variableInitializer = argument.argumentExpression.transform(substitutor, data = null)
                val shouldCreateTemporaryVariable = argument.shouldBeSubstitutedViaTemporaryVariable()

                if (shouldCreateTemporaryVariable) {
                    val newVariable = createTemporaryVariable(parameter, variableInitializer, argument.isDefaultArg, callee)
                    if (argument.isDefaultArg) evaluationStatementsFromDefault.add(newVariable) else evaluationStatements.add(newVariable)
                    substituteMap[parameter] = irGetValueWithoutLocation(newVariable.symbol)
                    return@forEach
                }

                substituteMap[parameter] = if (variableInitializer is IrGetValue) {
                    irGetValueWithoutLocation(variableInitializer.symbol)
                } else {
                    variableInitializer
                }
            }

            // Next two composite blocks are used just as containers for two types of variables.
            // First one store temp variables that represent non default arguments of inline call and second one store defaults.
            // This is needed because these two groups of variables need slightly different processing on (JVM) backend.
            val blockForNewStatements = IrCompositeImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType,
                    LoweredStatementOrigins.INLINED_FUNCTION_ARGUMENTS, statements = evaluationStatements
            )

            val blockForNewStatementsFromDefault = IrCompositeImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType,
                    LoweredStatementOrigins.INLINED_FUNCTION_DEFAULT_ARGUMENTS, statements = evaluationStatementsFromDefault
            )

            return listOfNotNull(
                    blockForNewStatements.takeIf { evaluationStatements.isNotEmpty() },
                    blockForNewStatementsFromDefault.takeIf { evaluationStatementsFromDefault.isNotEmpty() }
            )
        }

        private fun ParameterToArgument.shouldBeSubstitutedViaTemporaryVariable(): Boolean =
                !(isImmutableVariableLoad && parameter.index >= 0) && !argumentExpression.isPure(false, context = context)

        private fun createTemporaryVariable(
                parameter: IrValueParameter,
                variableInitializer: IrExpression,
                isDefaultArg: Boolean,
                callee: IrFunction
        ): IrVariable {
            val variable = currentScope.scope.createTemporaryVariable(
                    irExpression = IrBlockImpl(
                            if (isDefaultArg) variableInitializer.startOffset else UNDEFINED_OFFSET,
                            if (isDefaultArg) variableInitializer.endOffset else UNDEFINED_OFFSET,
                            // If original type of parameter is T, then `parameter.type` is T after substitution or erasure,
                            // depending on whether T reified or not.
                            parameter.type
                    ).apply {
                        statements.add(variableInitializer)
                    },
                    nameHint = callee.symbol.owner.name.asStringStripSpecialMarkers(),
                    isMutable = false,
                    origin = if (parameter == callee.extensionReceiverParameter) {
                        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER
                    } else {
                        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER
                    }
            )

            variable.name = Name.identifier(parameter.name.asStringStripSpecialMarkers())

            return variable
        }
    }

    private fun irGetValueWithoutLocation(
            symbol: IrValueSymbol,
            origin: IrStatementOrigin? = null,
    ): IrGetValue {
        return IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, origin).also {
            elementsWithLocationToPatch += it
        }
    }
}

internal class DeepCopyIrTreeWithSymbolsForInliner(
        val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
        val parent: IrDeclarationParent?,
        val devirtualizedCallSites: Map<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>,
        val copiedDevirtualizedCallSites: MutableMap<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>,
) {
    fun copy(irElement: IrElement): IrElement {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null)

        result.patchDeclarationParents(parent)
        return result
    }

    private inner class InlinerTypeRemapper(
            val symbolRemapper: SymbolRemapper,
            val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
    ) : TypeRemapper {
        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

        override fun leaveScope() {}

        private fun remapTypeArguments(
                arguments: List<IrTypeArgument>,
                erasedParameters: MutableSet<IrTypeParameterSymbol>?,
        ) =
                arguments.memoryOptimizedMap { argument ->
                    (argument as? IrTypeProjection)?.let { proj ->
                        remapType(proj.type, erasedParameters)?.let { newType ->
                            makeTypeProjection(newType, proj.variance)
                        } ?: IrStarProjectionImpl
                    }
                            ?: argument
                }

        override fun remapType(type: IrType) =
                remapType(type, mutableSetOf()) ?: error("Cannot substitute type ${type.render()}")

        private fun remapType(type: IrType, erasedParameters: MutableSet<IrTypeParameterSymbol>?): IrType? {
            if (type !is IrSimpleType) return type

            val classifier = type.classifier
            val substitutedType = typeArguments?.get(classifier)

            // Erase non-reified type parameter if asked to.
            if (erasedParameters != null && substitutedType != null && (classifier as? IrTypeParameterSymbol)?.owner?.isReified == false) {
                if (classifier in erasedParameters) {
                    return null
                }

                erasedParameters.add(classifier)

                // Pick the (necessarily unique) non-interface upper bound if it exists.
                val superTypes = classifier.owner.superTypes
                val superClass = superTypes.firstOrNull {
                    it.classOrNull?.owner?.isInterface == false
                }

                val upperBound = superClass ?: superTypes.first()

                // TODO: Think about how to reduce complexity from k^N to N^k
                val erasedUpperBound = remapType(upperBound, erasedParameters)
                        ?: error("Cannot erase upperbound ${upperBound.render()}")

                erasedParameters.remove(classifier)

                return erasedUpperBound.mergeNullability(type)
            }

            if (substitutedType is IrDynamicType) return substitutedType

            if (substitutedType is IrSimpleType) {
                return substitutedType.mergeNullability(type)
            }

            return type.buildSimpleType {
                kotlinType = null
                this.classifier = symbolRemapper.getReferencedClassifier(classifier)
                arguments = remapTypeArguments(type.arguments, erasedParameters)
                annotations = type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
            }
        }
    }

    private class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper) : DeepCopySymbolRemapper(descriptorsRemapper) {
        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    private val symbolRemapper = SymbolRemapperImpl(NullDescriptorsRemapper)
    private val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments)
    private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
        private fun IrType.erase() = typeRemapper.remapType(this)

        override fun visitCall(expression: IrCall) = super.visitCall(expression).also { copiedCall ->
            val devirtualizedCallSite = devirtualizedCallSites[expression]
            if (devirtualizedCallSite != null)
                copiedDevirtualizedCallSites[copiedCall] = devirtualizedCallSite
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall) =
                IrTypeOperatorCallImpl(
                        expression.startOffset, expression.endOffset,
                        expression.type.erase(),
                        expression.operator,
                        expression.typeOperand.erase(),
                        expression.argument.transform()
                ).copyAttributes(expression)
    }
}
