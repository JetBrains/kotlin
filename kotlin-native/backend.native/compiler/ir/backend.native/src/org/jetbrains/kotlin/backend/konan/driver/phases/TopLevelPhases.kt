/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.hasAnnotation
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
        psiToIrEngine.runSpecialBackendChecks(output.irModule, output.symbols)
        output to additionalOutput
    }
    runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput.irModule)
    return psiToIrOutput to additionalOutput
}

internal fun <C : PhaseContext> PhaseEngine<C>.runBackend(backendContext: Context, irModule: IrModuleFragment) {
    useContext(backendContext) { backendEngine ->
        backendEngine.runPhase(functionsWithoutBoundCheck)
        val fragments = backendEngine.splitIntoFragments(irModule)
        fragments.forEach { (generationState, fragment) ->
            backendEngine.useContext(generationState) { generationStateEngine ->
                generationStateEngine.runLowerAndCompile(fragment)
            }
        }
    }
}

private fun isReferencedByNativeRuntime(declarations: List<IrDeclaration>): Boolean =
        declarations.any {
            it.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)
                    || it.hasAnnotation(RuntimeNames.exportForCppRuntime)
        } || declarations.any {
            it is IrClass && isReferencedByNativeRuntime(it.declarations)
        }

private fun PhaseEngine<out Context>.splitIntoFragments(
        input: IrModuleFragment,
): Sequence<Pair<NativeGenerationState, IrModuleFragment>> = if (context.config.producePerFileCache) {
    val module = input
    val files = module.files.toList()
    val stdlibIsBeingCached = module.descriptor == context.stdlibModule
    val functionInterfaceFiles = files.filter { it.isFunctionInterfaceFile }
    val filesReferencedByNativeRuntime = files.takeIf { stdlibIsBeingCached }
            ?.filter { isReferencedByNativeRuntime(it.declarations) }.orEmpty()

    files.asSequence().filter { !it.isFunctionInterfaceFile }.map { file ->
        val generationState = NativeGenerationState(
                context.config,
                context,
                CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())
        )
        val fragment = IrModuleFragmentImpl(input.descriptor, input.irBuiltins, listOf(file))
        if (generationState.shouldDefineFunctionClasses)
            fragment.files += functionInterfaceFiles

        if (generationState.shouldLinkRuntimeNativeLibraries) {
            filesReferencedByNativeRuntime.forEach {
                generationState.dependenciesTracker.add(it)
            }
        }

        fragment.files.filterIsInstance<IrFileImpl>().forEach {
            it.module = fragment
        }
        generationState to fragment
    }
} else {
    val nativeGenerationState = NativeGenerationState(context.config, context, context.config.libraryToCache?.strategy)
    sequenceOf(nativeGenerationState to input)
}

/**
 * Performs all the hard work:
 * 1. Runs IR lowerings
 * 2. Runs LTO.
 * 3. Translates IR to LLVM IR.
 * 4. Optimizes it.
 * 5. Serializes it to a bitcode file.
 * 6. Compiles bitcode to an object file.
 * 7. Performs binary linkage.
 * ... And stores additional cache info.
 *
 * TODO: Split into more granular phases with explicit inputs and outputs.
 */
internal fun PhaseEngine<NativeGenerationState>.runLowerAndCompile(module: IrModuleFragment) {
    if (context.config.produce.isCache) {
        runPhase(BuildAdditionalCacheInfoPhase, module)
    }
    if (context.config.produce == CompilerOutputKind.PROGRAM) {
        runPhase(EntryPointPhase, module)
    }
    runBackendCodegen(module)
    if (context.config.produce.isCache) {
        runPhase(SaveAdditionalCacheInfoPhase)
    }
    val bitcodeFile = runPhase(WriteBitcodeFilePhase, context.llvm.module)
    val objectFiles = runPhase(ObjectFilesPhase, bitcodeFile)
    runPhase(LinkerPhase, objectFiles)
    if (context.config.produce.isCache) {
        runPhase(FinalizeCachePhase)
    }
}

internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment) {
    runAllLowerings(module)
    lowerDependencies(module)
    runCodegen(module)
    runPhase(CStubsPhase)
    // TODO: Consider extracting llvmModule and friends from nativeGenerationState and pass them explicitly.
    //  Motivation: possibility to run LTO on bitcode level after separate IR compilation.
    val llvmModule = context.llvm.module
    if (context.config.needCompilerVerification || context.config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)) {
        runPhase(VerifyBitcodePhase, llvmModule)
    }
    if (context.shouldPrintBitCode()) {
        runPhase(PrintBitcodePhase, llvmModule)
    }
    runPhase(LinkBitcodeDependenciesPhase)
    runBitcodePostProcessing(llvmModule)
}

/**
 * Compile lowered [module] to object file.
 * @return absolute path to object file.
 */
private fun PhaseEngine<NativeGenerationState>.runCodegen(module: IrModuleFragment) {
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

/**
 * Lowers and links to [inputModule] dependencies of the current compilation target.
 */
private fun PhaseEngine<NativeGenerationState>.lowerDependencies(inputModule: IrModuleFragment) {
    val files = mutableListOf<IrFile>()
    files += inputModule.files
    inputModule.files.clear()

    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM?
    context.config.librariesWithDependencies()
            .reversed()
            .forEach {
                val libModule = context.context.irModules[it.libraryName]
                if (libModule == null || !context.llvmModuleSpecification.containsModule(libModule))
                    return@forEach
                runAllLowerings(libModule)
            }

    // Save all files for codegen in reverse topological order.
    // This guarantees that libraries initializers are emitted in correct order.
    context.config.librariesWithDependencies()
            .forEach {
                val libModule = context.context.irModules[it.libraryName]
                if (libModule == null || !context.llvmModuleSpecification.containsModule(libModule))
                    return@forEach

                inputModule.files += libModule.files
            }

    inputModule.files += files
}