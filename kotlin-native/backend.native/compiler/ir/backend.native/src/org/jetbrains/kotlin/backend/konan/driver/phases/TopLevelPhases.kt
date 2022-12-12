/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.lower.UnboxInlineLowering
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal fun PhaseEngine<PhaseContext>.runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
    val frontendOutput = useContext(FrontendContextImpl(config)) { it.runFrontend(environment) }
    return frontendOutput as? FrontendPhaseOutput.Full
}

internal fun PhaseEngine<PhaseContext>.runPsiToIr(
        frontendOutput: FrontendPhaseOutput.Full,
        isProducingLibrary: Boolean,
): PsiToIrOutput = runPsiToIr(frontendOutput, isProducingLibrary, {}).first

internal fun <T> PhaseEngine<PhaseContext>.runPsiToIr(
        frontendOutput: FrontendPhaseOutput.Full,
        isProducingLibrary: Boolean,
        produceAdditionalOutput: (PhaseEngine<out PsiToIrContext>) -> T
): Pair<PsiToIrOutput, T> {
    val config = this.context.config
    val psiToIrContext = PsiToIrContextImpl(config, frontendOutput.moduleDescriptor, frontendOutput.bindingContext)
    val (psiToIrOutput, additionalOutput) = useContext(psiToIrContext) { psiToIrEngine ->
        val additionalOutput = produceAdditionalOutput(psiToIrEngine)
        val psiToIrInput = PsiToIrInput(frontendOutput.moduleDescriptor, frontendOutput.environment, isProducingLibrary)
        val output = psiToIrEngine.runPhase(PsiToIrPhase, psiToIrInput)
        psiToIrEngine.runSpecialBackendChecks(output)
        output to additionalOutput
    }
    runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput.irModule)
    return psiToIrOutput to additionalOutput
}

internal fun <C : PhaseContext> PhaseEngine<C>.runBackend(backendContext: Context, module: IrModuleFragment) {
    useContext(backendContext) { backendEngine ->
        backendEngine.runPhase(functionsWithoutBoundCheck)
        backendEngine.processModuleFragments(module) { generationState, fragment ->
            backendEngine.useContext(generationState) { generationStateEngine ->
                // TODO: We can run compile part in parallel if we get rid of context.generationState.
                println("Compiling fragment with files ${fragment.files.joinToString { it.name }}")
                generationStateEngine.runLowerings(fragment)
                val bitcodeFileName = generationStateEngine.runCodegen(fragment)
                generationStateEngine.produceBinary(bitcodeFileName)
            }
        }
    }
}

internal fun PhaseEngine<out Context>.processModuleFragments(
        input: IrModuleFragment,
        action: (NativeGenerationState, IrModuleFragment) -> Unit
): Unit = if (context.config.producePerFileCache) {
    val module = input
    val files = module.files.toList()
    val functionInterfaceFiles = files.filter { it.isFunctionInterfaceFile }

    for (file in files) {
        if (file.isFunctionInterfaceFile) continue
        val generationState = NativeGenerationState(
                context.config,
                context,
                CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())
        )
        val m1 = IrModuleFragmentImpl(input.descriptor, input.irBuiltins, listOf(file))
        if (generationState.shouldDefineFunctionClasses)
            m1.files += functionInterfaceFiles
        m1.files.filterIsInstance<IrFileImpl>().forEach {
            it.module = m1
        }
        action(generationState, m1)
    }
} else {
    val nativeGenerationState = NativeGenerationState(context.config, context, context.config.libraryToCache?.strategy)
    action(nativeGenerationState, input)
}

internal fun PhaseEngine<NativeGenerationState>.runLowerings(module: IrModuleFragment) {
    if (context.config.produce.isCache) {
        runPhase(BuildAdditionalCacheInfoPhase, module)
    }
    if (context.config.produce == CompilerOutputKind.PROGRAM) {
        runPhase(EntryPointPhase, module)
    }
    runAllLowerings(module)
    if (!context.config.produce.isCache) {
        lowerDependencies(module)
    }
}

internal fun PhaseEngine<NativeGenerationState>.runCodegen(module: IrModuleFragment): String {
    runBackendCodegen(module)
    runBitcodePostProcessing()
    if (context.config.produce.isCache) {
        runPhase(SaveAdditionalCacheInfoPhase)
    }
    return runPhase(WriteBitcodeFilePhase, context.llvm.module)
}

internal fun PhaseEngine<NativeGenerationState>.produceBinary(bitcodeFileName: String) {
    val objectFiles = runPhase(ObjectFilesPhase, bitcodeFileName)
    runPhase(LinkerPhase, objectFiles)
    if (context.config.produce.isCache) {
        runPhase(SaveAdditionalCacheInfoPhase)
    }
}


internal val PropertyAccessorInlinePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "PropertyAccessorInline",
        description = "Property accessor inline lowering"
) { context, irFile ->
    PropertyAccessorInlineLowering(context.context).lower(irFile)
}

internal val UnboxInlinePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "UnboxInline",
        description = "Unbox functions inline lowering"
) { context, irFile ->
    UnboxInlineLowering(context.context).lower(irFile)
}

internal fun PhaseEngine<NativeGenerationState>.runBitcodePhases(module: IrModuleFragment) {
    val optimize = context.shouldOptimize()
    module.files.forEach {
        runPhase(ReturnsInsertionPhase, it)
    }
    val moduleDFG = runPhase(BuildDFGPhase, module, disable = !optimize)
    val devirtualizationAnalysisResults = runPhase(DevirtualizationAnalysisPhase, DevirtualizationAnalysisInput(module, moduleDFG), disable = !optimize)
    val dceResult = runPhase(DCEPhase, DCEInput(module, moduleDFG, devirtualizationAnalysisResults), disable = !optimize)
    runPhase(RemoveRedundantCallsToStaticInitializersPhase, RedundantCallsInput(moduleDFG, devirtualizationAnalysisResults, module), disable = !optimize)
    runPhase(DevirtualizationPhase, DevirtualizationInput(module, devirtualizationAnalysisResults), disable = !optimize)
    // Have to run after link dependencies phase, because fields from dependencies can be changed during lowerings.
    // Inline accessors only in optimized builds due to separate compilation and possibility to get broken debug information.
    module.files.forEach {
        runPhase(PropertyAccessorInlinePhase, it, disable = !optimize)
        runPhase(InlineClassPropertyAccessorsPhase, it, disable = !optimize)
        runPhase(RedundantCoercionsCleaningPhase, it)
        // depends on redundantCoercionsCleaningPhase
        runPhase(UnboxInlinePhase, it, disable = !optimize)

    }
    runPhase(CreateLLVMDeclarationsPhase, module)
    runPhase(GHAPhase, module, disable = !optimize)
    runPhase(RTTIPhase, RTTIInput(module, dceResult))
    val lifetimes = runPhase(EscapeAnalysisPhase, EscapeAnalysisInput(module, moduleDFG, devirtualizationAnalysisResults), disable = !optimize)
    runPhase(CodegenPhase, CodegenInput(module, lifetimes))
}

internal fun PhaseEngine<NativeGenerationState>.lowerDependencies(input: IrModuleFragment) {
    val parent = context.context
    val files = mutableListOf<IrFile>()
    files += input.files
    input.files.clear()

    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    context.config.librariesWithDependencies(input.descriptor)
            .reversed()
            .forEach {
                val libModule = parent.irModules[it.libraryName]
                if (libModule == null || !context.llvmModuleSpecification.containsModule(libModule))
                    return@forEach
                runAllLowerings(libModule)
            }

    // Save all files for codegen in reverse topological order.
    // This guarantees that libraries initializers are emitted in correct order.
    context.config.librariesWithDependencies(input.descriptor)
            .forEach {
                val libModule = parent.irModules[it.libraryName]
                if (libModule == null || !context.llvmModuleSpecification.containsModule(libModule))
                    return@forEach
                input.files += libModule.files
            }

    input.files += files
}

internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment) {
    runBitcodePhases(module)
    runPhase(CStubsPhase)
    if (context.config.needCompilerVerification || context.config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)) {
        runPhase(VerifyBitcodePhase)
    }
    runPhase(PrintBitcodePhase)
    runPhase(LinkBitcodeDependenciesPhase)
}