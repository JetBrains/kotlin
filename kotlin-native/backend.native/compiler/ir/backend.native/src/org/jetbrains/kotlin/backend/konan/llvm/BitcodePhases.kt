/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysis
import org.jetbrains.kotlin.backend.konan.llvm.coverage.runCoveragePass
import org.jetbrains.kotlin.backend.konan.lower.InlineClassPropertyAccessorsLowering
import org.jetbrains.kotlin.backend.konan.lower.RedundantCoercionsCleaner
import org.jetbrains.kotlin.backend.konan.lower.ReturnsInsertionLowering
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val createLLVMDeclarationsPhase = makeKonanModuleOpPhase(
        name = "CreateLLVMDeclarations",
        description = "Map IR declarations to LLVM",
        op = { context, _ -> context.generationState.llvmDeclarations = createLlvmDeclarations(context.generationState, context.ir.irModule) }
)

internal val RTTIPhase = makeKonanModuleOpPhase(
        name = "RTTI",
        description = "RTTI generation",
        op = { context, irModule ->
            val visitor = RTTIGeneratorVisitor(context.generationState)
            irModule.acceptVoid(visitor)
            visitor.dispose()
        }
)

internal val buildDFGPhase = makeKonanModuleOpPhase(
        name = "BuildDFG",
        description = "Data flow graph building",
        op = { context, irModule ->
            context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
        }
)

internal val returnsInsertionPhase = makeKonanModuleOpPhase(
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        //prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase), TODO: if there are no files in the module, this requirement fails.
        op = { context, irModule -> irModule.files.forEach { ReturnsInsertionLowering(context).lower(it) } }
)

internal val inlineClassPropertyAccessorsPhase = makeKonanModuleOpPhase(
        name = "InlineClassPropertyAccessorsLowering",
        description = "Inline class property accessors",
        op = { context, irModule -> irModule.files.forEach { InlineClassPropertyAccessorsLowering(context).lower(it) } }
)

internal val devirtualizationAnalysisPhase = makeKonanModuleOpPhase(
        name = "DevirtualizationAnalysis",
        description = "Devirtualization analysis",
        prerequisite = setOf(buildDFGPhase),
        op = { context, _ ->
            context.devirtualizationAnalysisResult = DevirtualizationAnalysis.run(
                    context, context.moduleDFG!!, ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            )
        }
)

internal val redundantCoercionsCleaningPhase = makeKonanModuleOpPhase(
        name = "RedundantCoercionsCleaning",
        description = "Redundant coercions cleaning",
        op = { context, irModule -> irModule.files.forEach { RedundantCoercionsCleaner(context).lower(it) } }
)

internal val ghaPhase = makeKonanModuleOpPhase(
        name = "GHAPhase",
        description = "Global hierarchy analysis",
        op = { context, irModule -> GlobalHierarchyAnalysis(context, irModule).run() }
)

internal val IrFunction.longName: String
        get() = "${(parent as? IrClass)?.name?.asString() ?: "<root>"}.${(this as? IrSimpleFunction)?.name ?: "<init>"}"

internal val dcePhase = makeKonanModuleOpPhase(
        name = "DCEPhase",
        description = "Dead code elimination",
        prerequisite = setOf(devirtualizationAnalysisPhase),
        op = { context, _ ->
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

            val callGraph = CallGraphBuilder(
                    context, context.moduleDFG!!,
                    externalModulesDFG,
                    context.devirtualizationAnalysisResult!!,
                    // For DCE we don't wanna miss any potentially reachable function.
                    nonDevirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE
            ).build()

            val referencedFunctions = mutableSetOf<IrFunction>()
            callGraph.rootExternalFunctions.forEach {
                if (!it.isStaticFieldInitializer)
                    referencedFunctions.add(it.irFunction ?: error("No IR for: $it"))
            }
            for (node in callGraph.directEdges.values) {
                if (!node.symbol.isStaticFieldInitializer)
                    referencedFunctions.add(node.symbol.irFunction ?: error("No IR for: ${node.symbol}"))
                node.callSites.forEach {
                    assert (!it.isVirtual) { "There should be no virtual calls in the call graph, but was: ${it.actualCallee}" }
                    referencedFunctions.add(it.actualCallee.irFunction ?: error("No IR for: ${it.actualCallee}"))
                }
            }

            context.irModule!!.acceptChildrenVoid(object: IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    // TODO: Generalize somehow, not that graceful.
                    if (declaration.name == OperatorNameConventions.INVOKE
                            && declaration.parent.let { it is IrClass && it.defaultType.isFunction() }) {
                        referencedFunctions.add(declaration)
                    }
                    super.visitFunction(declaration)
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    // TODO: NativePointed is the only inline class for which the field's type and
                    //       the constructor parameter's type are different.
                    //       Thus we need to conserve the constructor no matter if it was actually referenced somehow or not.
                    //       See [IrTypeInlineClassesSupport.getInlinedClassUnderlyingType] why.
                    if (declaration.parentAsClass.name.asString() == InteropFqNames.nativePointedName && declaration.isPrimary)
                        referencedFunctions.add(declaration)
                    super.visitConstructor(declaration)
                }
            })

            context.irModule!!.transformChildrenVoid(object: IrElementTransformerVoid() {
                override fun visitFile(declaration: IrFile): IrFile {
                    declaration.declarations.removeAll {
                        (it is IrFunction && !referencedFunctions.contains(it))
                    }
                    return super.visitFile(declaration)
                }

                override fun visitClass(declaration: IrClass): IrStatement {
                    if (declaration == context.ir.symbols.nativePointed)
                        return super.visitClass(declaration)
                    declaration.declarations.removeAll {
                        (it is IrFunction && it.isReal && !referencedFunctions.contains(it))
                    }
                    return super.visitClass(declaration)
                }

                override fun visitProperty(declaration: IrProperty): IrStatement {
                    if (declaration.getter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                        declaration.getter = null
                    }
                    if (declaration.setter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                        declaration.setter = null
                    }
                    return super.visitProperty(declaration)
                }
            })

            context.referencedFunctions = referencedFunctions
        }
)

internal val removeRedundantCallsToStaticInitializersPhase = makeKonanModuleOpPhase(
        name = "RemoveRedundantCallsToStaticInitializersPhase",
        description = "Redundant static initializers calls removal",
        prerequisite = setOf(devirtualizationAnalysisPhase),
        op = { context, _ ->
            val moduleDFG = context.moduleDFG!!
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

            val callGraph = CallGraphBuilder(
                    context, moduleDFG,
                    externalModulesDFG,
                    context.devirtualizationAnalysisResult!!,
                    nonDevirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE
            ).build()

            val rootSet = DevirtualizationAnalysis.computeRootSet(context, moduleDFG, externalModulesDFG)
                    .mapNotNull { it.irFunction }
                    .toSet()

            StaticInitializersOptimization.removeRedundantCalls(context, callGraph, rootSet)
        }
)

internal val devirtualizationPhase = makeKonanModuleOpPhase(
        name = "Devirtualization",
        description = "Devirtualization",
        prerequisite = setOf(buildDFGPhase, devirtualizationAnalysisPhase),
        op = { context, irModule ->
            val devirtualizedCallSites =
                    context.devirtualizationAnalysisResult!!.devirtualizedCallSites
                            .asSequence()
                            .filter { it.key.irCallSite != null }
                            .associate { it.key.irCallSite!! to it.value }
            DevirtualizationAnalysis.devirtualize(irModule, context,
                    ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap()), devirtualizedCallSites)
        }
)

internal val escapeAnalysisPhase = makeKonanModuleOpPhase(
        name = "EscapeAnalysis",
        description = "Escape analysis",
        prerequisite = setOf(buildDFGPhase, devirtualizationAnalysisPhase),
        op = { context, _ ->
            val entryPoint = context.ir.symbols.entryPoint?.owner
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            val nonDevirtualizedCallSitesUnfoldFactor =
                    if (entryPoint != null) {
                        // For a final program it can be safely assumed that what classes we see is what we got,
                        // so can take those. In theory we can always unfold call sites using type hierarchy, but
                        // the analysis might converge much, much slower, so take only reasonably small for now.
                        5
                    }
                    else {
                        // Can't tolerate any non-devirtualized call site for a library.
                        // TODO: What about private virtual functions?
                        // Note: 0 is also bad - this means that there're no inheritors in the current source set,
                        // but there might be some provided by the users of the library being produced.
                        -1
                    }
            val callGraph = CallGraphBuilder(
                    context, context.moduleDFG!!,
                    externalModulesDFG,
                    context.devirtualizationAnalysisResult!!,
                    nonDevirtualizedCallSitesUnfoldFactor
            ).build()
            EscapeAnalysis.computeLifetimes(
                    context, context.moduleDFG!!, externalModulesDFG, callGraph, context.lifetimes
            )
        }
)

internal val codegenPhase = makeKonanModuleOpPhase(
        name = "Codegen",
        description = "Code generation",
        op = { context, irModule ->
            context.generationState.objCExport = ObjCExport(
                    context.generationState,
                    context.moduleDescriptor,
                    context.objCExportedInterface,
                    context.objCExportCodeSpec
            )

            irModule.acceptVoid(CodeGeneratorVisitor(context.generationState, context.lifetimes))

            if (context.generationState.hasDebugInfo())
                DIFinalize(context.generationState.debugInfo.builder)
        }
)

internal val cStubsPhase = makeKonanModuleOpPhase(
        name = "CStubs",
        description = "C stubs compilation",
        op = { context, _ -> produceCStubs(context.generationState) }
)

internal val linkBitcodeDependenciesPhase = makeKonanModuleOpPhase(
        name = "LinkBitcodeDependencies",
        description = "Link bitcode dependencies",
        op = { context, _ -> linkBitcodeDependencies(context.generationState) }
)

internal val checkExternalCallsPhase = makeKonanModuleOpPhase(
        name = "CheckExternalCalls",
        description = "Check external calls",
        op = { context, _ -> checkLlvmModuleExternalCalls(context.generationState) }
)

internal val rewriteExternalCallsCheckerGlobals = makeKonanModuleOpPhase(
        name = "RewriteExternalCallsCheckerGlobals",
        description = "Rewrite globals for external calls checker after optimizer run",
        op = { context, _ -> addFunctionsListSymbolForChecker(context.generationState) }
)



internal val bitcodeOptimizationPhase = makeKonanModuleOpPhase(
        name = "BitcodeOptimization",
        description = "Optimize bitcode",
        op = { context, _ ->
            val generationState = context.generationState
            val config = createLTOFinalPipelineConfig(context.generationState)
            LlvmOptimizationPipeline(config, context.generationState.llvm.module, generationState).use {
                it.run()
            }
        }
)

internal val coveragePhase = makeKonanModuleOpPhase(
        name = "Coverage",
        description = "Produce coverage information",
        op = { context, _ -> runCoveragePass(context.generationState) }
)

internal val optimizeTLSDataLoadsPhase = makeKonanModuleOpPhase(
        name = "OptimizeTLSDataLoads",
        description = "Optimize multiple loads of thread data",
        op = { context, _ -> removeMultipleThreadDataLoads(context.generationState) }
)

internal val removeRedundantSafepointsPhase = makeKonanModuleOpPhase(
        name = "RemoveRedundantSafepoints",
        description = "Remove function prologue safepoints inlined to another function",
        op = { context, _ ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = context.generationState.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val verifyBitcodePhase = makeKonanModuleOpPhase(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context, _ -> context.verifyBitCode() }
)

internal val printBitcodePhase = makeKonanModuleOpPhase(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { context, _ ->
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
)