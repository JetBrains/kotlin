/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.utilities.*
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File

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

internal fun <C : PhaseContext> PhaseEngine<C>.runBackend(
        backendContext: Context,
        irModule: IrModuleFragment,
        compilationSessionFiles: CompilationFiles
) {
    val config = context.config
    useContext(backendContext) { backendEngine ->
        backendEngine.runPhase(functionsWithoutBoundCheck)
        val fragments = backendEngine.splitIntoFragments(irModule)
        fragments.forEach { fragment ->
            val tempFiles = createTempFiles(config, fragment.cacheDeserializationStrategy)
            try {
                val outputPath = config.cacheSupport.tryGetImplicitOutput(fragment.cacheDeserializationStrategy) ?: config.outputPath
                val outputFiles = OutputFiles(outputPath, config.target, config.produce)
                val cacheFileName = if (context.config.producePerFileCache) {
                    outputFiles.perFileCacheFileName
                } else {
                    outputFiles.cacheFileName
                }
                // TODO: Currently, all llvm modules are named as "out" which might lead to collisions.
                val llvmModuleName = "out"
                val moduleCompilationFiles = createModuleCompilationFiles(config, tempFiles, compilationSessionFiles, llvmModuleName)
                val (objectFile, cacheBinaries, dependenciesTrackingResult) =
                        backendEngine.compileIrToObjectFile(fragment, moduleCompilationFiles, llvmModuleName, cacheFileName)
                link(objectFile, cacheBinaries, dependenciesTrackingResult, moduleCompilationFiles, isCoverageEnabled = false)
                moduleCompilationFiles.getComponentOrNull<CompilationFiles.Component.CacheDirectories>()?.let { cacheDirectories ->
                    runPhase(FinalizeCachePhase, FinalizeCacheInput(cacheDirectories.tempCacheDirectory, cacheDirectories.outputDirectory))
                }
            } finally {
                tempFiles.dispose()
            }
        }
    }
}

private fun PhaseEngine<out Context>.compileIrToObjectFile(
        fragment: BackendJobFragment,
        compilationFiles: CompilationFiles,
        llvmModuleName: String,
        cacheFileName: String,
): Triple<File, ResolvedCacheBinaries, DependenciesTrackingResult> {
    val generationState = NativeGenerationState(
            context.config,
            context,
            fragment.cacheDeserializationStrategy,
            fragment.dependenciesTracker,
            fragment.llvmModuleSpecification,
            llvmModuleName = llvmModuleName,
            cacheFileName = cacheFileName,
            debugInfoFileName = compilationFiles.getComponent<CompilationFiles.Component.DebugInfo>().debugInfoFileName
    )
    return useContext(generationState) { generationStateEngine ->
        val bitcodeFile = compilationFiles.getComponentOrNull<CompilationFiles.Component.ModuleBitcode>()!!.file()
        // TODO: Make this work if we first compile all the fragments and only after that run the link phases.
        generationStateEngine.compileModule(fragment.irModule, bitcodeFile, compilationFiles)
        // Split here
        val moduleCompilationOutput = ModuleCompilationOutput(bitcodeFile, generationState.dependenciesTracker.collectResult())
        val (objectFile, cacheBinaries) = compileBitcode(
                moduleCompilationOutput.bitcodeFile,
                moduleCompilationOutput.dependenciesTrackingResult,
                compilationFiles
        )
        Triple(objectFile, cacheBinaries, moduleCompilationOutput.dependenciesTrackingResult)
    }
}


private fun isReferencedByNativeRuntime(declarations: List<IrDeclaration>): Boolean =
        declarations.any {
            it.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)
                    || it.hasAnnotation(RuntimeNames.exportForCppRuntime)
        } || declarations.any {
            it is IrClass && isReferencedByNativeRuntime(it.declarations)
        }

private data class BackendJobFragment(
        val irModule: IrModuleFragment,
        val cacheDeserializationStrategy: CacheDeserializationStrategy?,
        val dependenciesTracker: DependenciesTracker,
        val llvmModuleSpecification: LlvmModuleSpecification,
)

private fun PhaseEngine<out Context>.splitIntoFragments(
        input: IrModuleFragment,
): Sequence<BackendJobFragment> {
    val config = context.config
    return if (context.config.producePerFileCache) {
        val files = input.files.toList()
        val containsStdlib = config.libraryToCache!!.klib == context.stdlibModule.konanLibrary

        files.asSequence().filter { !it.isFunctionInterfaceFile }.map { file ->
            val cacheDeserializationStrategy = CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())
            val llvmModuleSpecification = CacheLlvmModuleSpecification(
                    config.cachedLibraries,
                    PartialCacheInfo(config.libraryToCache!!.klib, cacheDeserializationStrategy),
                    containsStdlib = containsStdlib
            )
            val dependenciesTracker = DependenciesTrackerImpl(llvmModuleSpecification, context.config, context)
            val fragment = IrModuleFragmentImpl(input.descriptor, input.irBuiltins, listOf(file))
            if (containsStdlib && cacheDeserializationStrategy.containsKFunctionImpl)
                fragment.files += files.filter { it.isFunctionInterfaceFile }

            if (containsStdlib && cacheDeserializationStrategy.containsRuntime) {
                files.filter { isReferencedByNativeRuntime(it.declarations) }
                        .forEach { dependenciesTracker.add(it) }
            }

            fragment.files.filterIsInstance<IrFileImpl>().forEach {
                it.module = fragment
            }
            BackendJobFragment(
                    fragment,
                    cacheDeserializationStrategy,
                    dependenciesTracker,
                    llvmModuleSpecification,
            )
        }
    } else {
        val llvmModuleSpecification = if (config.produce.isCache) {
            val containsStdlib = config.libraryToCache!!.klib == context.stdlibModule.konanLibrary
            CacheLlvmModuleSpecification(config.cachedLibraries, context.config.libraryToCache!!, containsStdlib = containsStdlib)
        } else {
            DefaultLlvmModuleSpecification(config.cachedLibraries)
        }
        sequenceOf(
                BackendJobFragment(
                        input,
                        context.config.libraryToCache?.strategy,
                        DependenciesTrackerImpl(llvmModuleSpecification, context.config, context),
                        llvmModuleSpecification
                )
        )
    }
}

internal data class ModuleCompilationOutput(
        val bitcodeFile: File,
        val dependenciesTrackingResult: DependenciesTrackingResult,
)

/**
 * 1. Runs IR lowerings
 * 2. Runs LTO.
 * 3. Translates IR to LLVM IR.
 * 4. Optimizes it.
 * 5. Serializes it to a bitcode file.
 */
internal fun PhaseEngine<NativeGenerationState>.compileModule(module: IrModuleFragment, bitcodeFile: File, compilationFiles: CompilationFiles) {
    if (context.config.produce.isCache) {
        runPhase(BuildAdditionalCacheInfoPhase, module)
    }
    if (context.config.produce == CompilerOutputKind.PROGRAM) {
        runPhase(EntryPointPhase, module)
    }
    runBackendCodegen(module, compilationFiles)
    runBitcodePostProcessing(compilationFiles)
    compilationFiles.getComponentOrNull<CompilationFiles.Component.CacheDirectories>()?.let { cacheDirectories ->
        runPhase(SaveAdditionalCacheInfoPhase, SaveAdditionalCacheInfoInput(cacheDirectories.tempCacheDirectory,
                context.dependenciesTracker.immediateBitcodeDependencies.toList(),
                context.inlineFunctionBodies.toList(),
                context.classFields.toList(),
                context.eagerInitializedFiles.toList(),
        ))
    }
    runPhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(context.llvm.module, bitcodeFile))
}

internal fun <C : PhaseContext> PhaseEngine<C>.compileBitcode(
        bitcodeFile: File,
        dependenciesTrackingResult: DependenciesTrackingResult,
        compilationFiles: CompilationFiles,
): Pair<File, ResolvedCacheBinaries> {
    val compilationResult = compilationFiles.getComponentOrNull<CompilationFiles.Component.ModuleObjectFile>()!!.file()
    runPhase(ObjectFilesPhase, ObjectFilesPhaseInput(bitcodeFile, compilationResult))
    val linkerOutputKind = determineLinkerOutput(context)
    return run {
        val resolvedCacheBinaries = resolveCacheBinaries(context.config.cachedLibraries, dependenciesTrackingResult)
        when {
            context.config.produce == CompilerOutputKind.STATIC_CACHE -> {
                compilationResult to ResolvedCacheBinaries(emptyList(), resolvedCacheBinaries.dynamic)
            }
            shouldPerformPreLink(context.config, resolvedCacheBinaries, linkerOutputKind) -> {
                val prelinkResult = compilationFiles.getComponentOrNull<CompilationFiles.Component.CachesPreLinkResult>()!!.file()
                runPhase(PreLinkCachesPhase, PreLinkCachesInput(listOf(compilationResult), resolvedCacheBinaries, prelinkResult))
                // Static caches are linked into binary, so we don't need to pass them.
                prelinkResult to ResolvedCacheBinaries(emptyList(), resolvedCacheBinaries.dynamic)
            }
            else -> {
                compilationResult to resolvedCacheBinaries
            }
        }
    }
}

internal fun <C : PhaseContext> PhaseEngine<C>.link(
        linkerInput: File,
        cacheBinaries: ResolvedCacheBinaries,
        dependenciesTrackingResult: DependenciesTrackingResult,
        compilationFiles: CompilationFiles,
        isCoverageEnabled: Boolean,
) {
    val linkerOutputKind = determineLinkerOutput(context)
    val installName: String? = compilationFiles.getComponentOrNull<CompilationFiles.Component.InstallName>()?.value
    val outputFile: File = compilationFiles.getComponentOrNull<CompilationFiles.Component.LinkerOutput>()?.value
            ?: context.reportCompilationError("Linker output was not specified, ")
    outputFile.delete()
    val linkerPhaseInput = LinkerPhaseInput(
            outputFile, linkerOutputKind, listOf(linkerInput.canonicalPath),
            dependenciesTrackingResult, cacheBinaries, isCoverageEnabled = isCoverageEnabled,
            "${outputFile.canonicalPath}.dSYM", installName
    )
    runPhase(LinkerPhase, linkerPhaseInput)
}


internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment, compilationFiles: CompilationFiles) {
    runAllLowerings(module)
    val dependenciesToCompile = findDependenciesToCompile()
    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM?
    dependenciesToCompile.reversed().forEach { irModule ->
        runAllLowerings(irModule)
    }
    mergeDependencies(module, dependenciesToCompile)
    runCodegen(module)
    val generatedBitcodeFiles = compilationFiles.getComponentOrNull<CompilationFiles.Component.CExport>()?.let { cexportFiles ->
        val input = CExportGenerateApiInput(
                context.context.cAdapterExportedElements!!,
                headerFile = cexportFiles.header,
                defFile = cexportFiles.def,
                cppAdapterFile = cexportFiles.cppAdapter
        )
        runPhase(CExportGenerateApiPhase, input)
        runPhase(CExportCompileAdapterPhase, CExportCompileAdapterInput(cexportFiles.cppAdapter, cexportFiles.bitcodeAdapter))
        listOf(cexportFiles.bitcodeAdapter)
    } ?: emptyList()

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
    runPhase(LinkBitcodeDependenciesPhase, generatedBitcodeFiles)
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