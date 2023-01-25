/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.impl.javaFile

internal fun PhaseEngine<PhaseContext>.runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
    val frontendOutput = useContext(FrontendContextImpl(config)) { it.runPhase(FrontendPhase, environment) }
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
                // TODO: Make this work if we first compile all the fragments and only after that run the link phases.
                val it = generationStateEngine.compileModule(fragment)
                // Split here
                compileAndLink(it, it.outputFiles.mainFileName, it.outputFiles, it.temporaryFiles, isCoverageEnabled = false)
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
    val functionInterfaceFiles = files.takeIf { stdlibIsBeingCached }
            ?.filter { it.isFunctionInterfaceFile }.orEmpty()
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

internal data class ModuleCompilationOutput(
        val bitcodeFile: String,
        val dependenciesTrackingResult: DependenciesTrackingResult,
        // Passing tempFiles and output files through this file looks silly and incorrect.
        // TODO: Refactor these classes and remove them from here.
        val temporaryFiles: TempFiles,
        val outputFiles: OutputFiles,
)

/**
 * 1. Runs IR lowerings
 * 2. Runs LTO.
 * 3. Translates IR to LLVM IR.
 * 4. Optimizes it.
 * 5. Serializes it to a bitcode file.
 */
internal fun PhaseEngine<NativeGenerationState>.compileModule(module: IrModuleFragment): ModuleCompilationOutput {
    if (context.config.produce.isCache) {
        runPhase(BuildAdditionalCacheInfoPhase, module)
    }
    if (context.config.produce == CompilerOutputKind.PROGRAM) {
        runPhase(EntryPointPhase, module)
    }
    runBackendCodegen(module)
    runBitcodePostProcessing()
    if (context.config.produce.isCache) {
        runPhase(SaveAdditionalCacheInfoPhase)
    }
    val bitcodeFile = context.tempFiles.nativeBinaryFile.javaFile()
    runPhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(context.llvm.module, bitcodeFile))
    val dependenciesTrackingResult = DependenciesTrackingResult(
            context.dependenciesTracker.bitcodeToLink,
            context.dependenciesTracker.allNativeDependencies,
            context.dependenciesTracker.allCachedBitcodeDependencies,
    )
    return ModuleCompilationOutput(bitcodeFile.canonicalPath, dependenciesTrackingResult, context.tempFiles, context.outputFiles)
}

internal fun <C : PhaseContext> PhaseEngine<C>.compileAndLink(
        moduleCompilationOutput: ModuleCompilationOutput,
        linkerOutputFile: String,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
        isCoverageEnabled: Boolean,
) {
    val objectFiles = runPhase(ObjectFilesPhase, ObjectFilesPhaseInput(moduleCompilationOutput.bitcodeFile, temporaryFiles))
    val linkerPhaseInput = LinkerPhaseInput(linkerOutputFile, objectFiles, moduleCompilationOutput.dependenciesTrackingResult,
            outputFiles, temporaryFiles, isCoverageEnabled = isCoverageEnabled)
    runPhase(LinkerPhase, linkerPhaseInput)
    if (context.config.produce.isCache) {
        runPhase(FinalizeCachePhase, outputFiles)
    }
}


internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment) {
    runAllLowerings(module)
    val dependenciesToCompile = findDependenciesToCompile()
    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM?
    dependenciesToCompile.reversed().forEach { irModule ->
        runAllLowerings(irModule)
    }
    mergeDependencies(module, dependenciesToCompile)
    runCodegen(module)
    if (context.config.produce.isNativeLibrary) {
        val input = CExportGenerateApiInput(
                context.context.cAdapterExportedElements!!,
                headerFile = context.outputFiles.cAdapterHeader.javaFile(),
                defFile = if (context.config.target.family == Family.MINGW) context.outputFiles.cAdapterDef.javaFile() else null,
                cppAdapterFile = context.tempFiles.cAdapterCpp.javaFile()
        )
        runPhase(CExportGenerateApiPhase, input)
    }
    runPhase(CStubsPhase)
    // TODO: Consider extracting llvmModule and friends from nativeGenerationState and pass them explicitly.
    //  Motivation: possibility to run LTO on bitcode level after separate IR compilation.
    val llvmModule = context.llvm.module
    // TODO: Consider dropping these in favor of proper phases dumping and validation.
    if (context.config.needCompilerVerification || context.config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)) {
        runPhase(VerifyBitcodePhase, llvmModule)
    }
    if (context.shouldPrintBitCode()) {
        runPhase(PrintBitcodePhase, llvmModule)
    }
    runPhase(LinkBitcodeDependenciesPhase)
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

private fun PhaseEngine<NativeGenerationState>.findDependenciesToCompile(): List<IrModuleFragment> {
    return context.config.librariesWithDependencies()
            .mapNotNull { context.context.irModules[it.libraryName] }
            .filter { context.llvmModuleSpecification.containsModule(it) }
}

// Save all files for codegen in reverse topological order.
// This guarantees that libraries initializers are emitted in correct order.
private fun mergeDependencies(targetModule: IrModuleFragment, dependencies: List<IrModuleFragment>) {
    targetModule.files.addAll(0, dependencies.flatMap { it.files })
}