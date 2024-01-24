/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.loops.LOWERED_FOR_LOOP
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.utilities.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.GlobalHierarchyAnalysis
import org.jetbrains.kotlin.backend.konan.ir.isVirtualCall
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.backend.konan.lower.AutoboxingTransformer
import org.jetbrains.kotlin.backend.konan.lower.BuiltinOperatorLowering
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ExternalModulesDFG
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFGBuilder
import org.jetbrains.kotlin.backend.konan.optimizations.dce
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*

internal val GHAPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "GHAPhase",
        description = "Global hierarchy analysis",
        op = { generationState, irModule ->
            GlobalHierarchyAnalysis(generationState.context, irModule).run()
        }
)

internal val BuildDFGPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment, ModuleDFG>(
        name = "BuildDFG",
        description = "Data flow graph building",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, generationState, _ ->
            val context = generationState.context
            val symbolTable = DataFlowIR.SymbolTable(context, DataFlowIR.Module())
            ModuleDFG(mutableMapOf(), symbolTable)
        },
        op = { generationState, irModule ->
            ModuleDFGBuilder(generationState, irModule).build()
        }
)

internal data class DevirtualizationAnalysisInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val DevirtualizationAnalysisPhase = createSimpleNamedCompilerPhase<NativeGenerationState, DevirtualizationAnalysisInput, DevirtualizationAnalysis.AnalysisResult>(
        name = "DevirtualizationAnalysis",
        description = "Devirtualization analysis",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ ->
            DevirtualizationAnalysis.AnalysisResult(
                    emptyMap(),
                    DevirtualizationAnalysis.DevirtualizationAnalysisImpl.EmptyTypeHierarchy
            )
        },
        op = { generationState, (irModule, moduleDFG) ->
            val context = generationState.context
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            DevirtualizationAnalysis.run(context, irModule, moduleDFG, externalModulesDFG)
        }
)

internal data class ForLoopsOverStdlibListsInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizedCallSites: MutableMap<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

private data class NonLoweredFor(
        val block: IrBlock,
        val declaration: IrDeclaration,
        val possibleCallees: List<DevirtualizationAnalysis.DevirtualizedCallee>?
)

internal val ForLoopsOverStdlibListsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, ForLoopsOverStdlibListsInput>(
        name = "ForLoopsOverStdlibLists",
        description = "For loops over stdlib lists lowering",
        op = { generationState, input ->
            val context = generationState.context
            val devirtualizedCallSites = input.devirtualizedCallSites
            input.irModule.files.forEach { irFile ->
                //val nonLoweredFors = mutableListOf<Pair<IrBlock, List<DevirtualizationAnalysis.DevirtualizedCallee>?>>()
                //val nonLoweredFors = mutableMapOf<IrBlock, List<DevirtualizationAnalysis.DevirtualizedCallee>?>()
                val nonLoweredFors = mutableListOf<NonLoweredFor>()
                irFile.acceptChildren(object : IrElementVisitor<Unit, IrDeclaration?> {
                    override fun visitElement(element: IrElement, data: IrDeclaration?) {
                        element.acceptChildren(this, data)
                    }

                    override fun visitVariable(declaration: IrVariable, data: IrDeclaration?) {
                        declaration.acceptChildren(this, data)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
                        declaration.acceptChildren(this, declaration)
                    }

                    override fun visitBlock(expression: IrBlock, data: IrDeclaration?) {
                        if (expression.origin == IrStatementOrigin.FOR_LOOP) {
                            val iteratorCall = (expression.statements[0] as? IrVariable)?.initializer as? IrCall
                            nonLoweredFors.add(
                                    NonLoweredFor(expression, data!!, iteratorCall?.let { devirtualizedCallSites[it]?.possibleCallees })
                            )
                        }

                        expression.acceptChildren(this, data)
                    }
                }, data = null)

//                val zzzchangedDeclarations = mutableSetOf<IrDeclaration>()
//                nonLoweredFors.forEach { (_, declaration, _) ->
//                    zzzchangedDeclarations.add(declaration)
//                }
//                zzzchangedDeclarations.forEach { declaration ->
//                    println("BEFORE: ${declaration.dump()}")
//                }

                ForLoopsLowering(
                        context,
                        { scopeOwnerSymbol: () -> IrSymbol ->
                            HeaderInfoForListsBuilder(context, devirtualizedCallSites, scopeOwnerSymbol, irFile.name.endsWith("bm4x.kt"))
                        },
                        KonanBCEForLoopBodyTransformer(context)
                ).lower(irFile)

                val changedDeclarations = mutableSetOf<IrDeclaration>()
//                nonLoweredFors.forEach { (block, declaration, _) ->
//                    if (block.origin == LOWERED_FOR_LOOP) {
//                        changedDeclarations.add(declaration)
//                    }
//                }
//                changedDeclarations.forEach { declaration ->
//                    println("AFTER for loops: ${declaration.dump()}")
//                }
                nonLoweredFors.forEach { (block, declaration, possibleCallees) ->
                    if (block.origin == LOWERED_FOR_LOOP) {
                        changedDeclarations.add(declaration)

                        val indexedObject = (block.statements[0] as IrComposite).statements[0] as IrVariable
//                        val autoboxingTransformer = AutoboxingTransformer(context)
                        block.transformChildrenVoid(object : IrElementTransformerVoid() {
                            override fun visitCall(expression: IrCall): IrExpression {
                                expression.transformChildrenVoid(this)

                                if ((expression.dispatchReceiver as? IrGetValue)?.symbol != indexedObject.symbol)
                                    return expression

                                val callee = expression.symbol.owner
                                if (possibleCallees != null && expression.isVirtualCall && devirtualizedCallSites[expression] == null) {
                                    val owner = callee.parentAsClass
                                    val callViaVtable = !owner.isInterface
                                    val layoutBuilder = context.getLayoutBuilder(owner)
                                    devirtualizedCallSites[expression] = DevirtualizationAnalysis.DevirtualizedCallSite(
                                            callee = input.moduleDFG.symbolTable.mapFunction(callee),
                                            possibleCallees = possibleCallees.map {
                                                val receiverType = it.receiverType as DataFlowIR.Type.Declared
                                                val actualCallee = if (callViaVtable)
                                                    receiverType.vtable[layoutBuilder.vtableIndex(callee)]
                                                else {
                                                    val itablePlace = layoutBuilder.itablePlace(callee)
                                                    receiverType.itable[itablePlace.interfaceId]!![itablePlace.methodIndex]
                                                }
                                                DevirtualizationAnalysis.DevirtualizedCallee(receiverType, actualCallee)
                                            }
                                    )
                                }

                                //return with(autoboxingTransformer) { expression.adaptIfNecessary(callee.returnType, expression.type) }
                                return expression
                            }
                        })
                    }
                }

                changedDeclarations.forEach { declaration ->
                    declaration.transform(BuiltinOperatorLowering(context), null) // TODO: Do it ad-hoc?

                    val body = when (declaration) {
                        is IrFunction -> {
                            context.logMultiple {
                                +"Analysing function ${declaration.render()}"
                                +"IR: ${declaration.dump()}"
                            }
                            declaration.body!!.also { body ->
                                LivenessAnalysis.run(body) { it is IrSuspensionPoint }
                                        .forEach { (irElement, liveVariables) ->
                                            generationState.liveVariablesAtSuspensionPoints[irElement as IrSuspensionPoint] = liveVariables
                                        }
                            }
                        }

                        is IrField -> {
                            context.logMultiple {
                                +"Analysing global field ${declaration.render()}"
                                +"IR: ${declaration.dump()}"
                            }
                            val initializer = declaration.initializer!!
                            IrSetFieldImpl(initializer.startOffset, initializer.endOffset, declaration.symbol, null,
                                    initializer.expression, context.irBuiltIns.unitType)
                        }

                        else -> error("Unexpected declaration: ${declaration.render()}")
                    }


//                    println("AFTER polishing: ${declaration.dump()}")
                    val function = FunctionDFGBuilder(generationState, input.moduleDFG.symbolTable).build(declaration, body)
                    input.moduleDFG.functions[function.symbol] = function
                }
            }
        },
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
)

internal data class DCEInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val DCEPhase = createSimpleNamedCompilerPhase<NativeGenerationState, DCEInput, Set<IrFunction>?>(
        name = "DCEPhase",
        description = "Dead code elimination",
        outputIfNotEnabled = { _, _, _, _ -> null },
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val context = generationState.context
            dce(context, input.irModule, input.moduleDFG, input.devirtualizationAnalysisResult)
        }
)

internal data class DevirtualizationInput(
        val irModule: IrModuleFragment,
        val devirtualizedCallSites: Map<IrCall, DevirtualizationAnalysis.DevirtualizedCallSite>
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val DevirtualizationPhase = createSimpleNamedCompilerPhase<NativeGenerationState, DevirtualizationInput>(
        name = "Devirtualization",
        description = "Devirtualization",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val context = generationState.context
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            DevirtualizationAnalysis.devirtualize(input.irModule, context,
                    externalModulesDFG, input.devirtualizedCallSites)
        }
)

internal data class EscapeAnalysisInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val EscapeAnalysisPhase = createSimpleNamedCompilerPhase<NativeGenerationState, EscapeAnalysisInput, Map<IrElement, Lifetime>>(
        name = "EscapeAnalysis",
        description = "Escape analysis",
        outputIfNotEnabled = { _, _, _, _ -> emptyMap() },
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val lifetimes = mutableMapOf<IrElement, Lifetime>()
            val context = generationState.context
            val entryPoint = context.ir.symbols.entryPoint?.owner
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            val nonDevirtualizedCallSitesUnfoldFactor =
                    if (entryPoint != null) {
                        // For a final program it can be safely assumed that what classes we see is what we got,
                        // so can take those. In theory we can always unfold call sites using type hierarchy, but
                        // the analysis might converge much, much slower, so take only reasonably small for now.
                        5
                    } else {
                        // Can't tolerate any non-devirtualized call site for a library.
                        // TODO: What about private virtual functions?
                        // Note: 0 is also bad - this means that there're no inheritors in the current source set,
                        // but there might be some provided by the users of the library being produced.
                        -1
                    }
            val callGraph = CallGraphBuilder(
                    context,
                    input.irModule,
                    input.moduleDFG,
                    externalModulesDFG,
                    input.devirtualizationAnalysisResult,
                    nonDevirtualizedCallSitesUnfoldFactor
            ).build()
            EscapeAnalysis.computeLifetimes(context, generationState, input.moduleDFG, externalModulesDFG, callGraph, lifetimes)
            lifetimes
        }
)

internal data class RedundantCallsInput(
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
        val irModule: IrModuleFragment,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val RemoveRedundantCallsToStaticInitializersPhase = createSimpleNamedCompilerPhase<NativeGenerationState, RedundantCallsInput>(
        name = "RemoveRedundantCallsToStaticInitializersPhase",
        description = "Redundant static initializers calls removal",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val context = generationState.context
            val moduleDFG = input.moduleDFG
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

            val callGraph = CallGraphBuilder(
                    context,
                    input.irModule,
                    moduleDFG,
                    externalModulesDFG,
                    input.devirtualizationAnalysisResult,
                    nonDevirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE
            ).build()

            val rootSet = DevirtualizationAnalysis.computeRootSet(context, input.irModule, moduleDFG, externalModulesDFG)
                    .mapNotNull { it.irFunction }
                    .toSet()

            StaticInitializersOptimization.removeRedundantCalls(context, input.irModule, callGraph, rootSet)
        }
)