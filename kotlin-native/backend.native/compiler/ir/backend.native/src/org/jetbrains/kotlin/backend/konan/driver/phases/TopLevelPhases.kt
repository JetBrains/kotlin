/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.utilities.CExportFiles
import org.jetbrains.kotlin.backend.konan.driver.utilities.createTempFiles
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
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
import org.jetbrains.kotlin.konan.file.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
    val config = context.config
    useContext(backendContext) { backendEngine ->
        backendEngine.runPhase(functionsWithoutBoundCheck)

        fun createGenerationStateAndRunLowerings(fragment: BackendJobFragment): NativeGenerationState {
            val outputPath = config.cacheSupport.tryGetImplicitOutput(fragment.cacheDeserializationStrategy) ?: config.outputPath
            val outputFiles = OutputFiles(outputPath, config.target, config.produce)
            val generationState = NativeGenerationState(context.config, backendContext,
                    fragment.cacheDeserializationStrategy, fragment.dependenciesTracker, fragment.llvmModuleSpecification, outputFiles,
                    llvmModuleName = "out" // TODO: Currently, all llvm modules are named as "out" which might lead to collisions.
            )
            try {
                val module = fragment.irModule
                newEngine(generationState) { generationStateEngine ->
                    if (context.config.produce.isCache) {
                        generationStateEngine.runPhase(BuildAdditionalCacheInfoPhase, module)
                    }
                    if (context.config.produce == CompilerOutputKind.PROGRAM) {
                        generationStateEngine.runPhase(EntryPointPhase, module)
                    }
                    generationStateEngine.lowerModuleWithDependencies(module)
                }
                return generationState
            } catch (t: Throwable) {
                generationState.dispose()
                throw t
            }
        }

        fun runAfterLowerings(fragment: BackendJobFragment, generationState: NativeGenerationState) {
            val tempFiles = createTempFiles(config, fragment.cacheDeserializationStrategy)
            val outputFiles = generationState.outputFiles
            try {
                backendEngine.useContext(generationState) { generationStateEngine ->
                    val bitcodeFile = tempFiles.create(generationState.llvmModuleName, ".bc").javaFile()
                    val cExportFiles = if (config.produce.isNativeLibrary) {
                        CExportFiles(
                                cppAdapter = tempFiles.create("api", ".cpp").javaFile(),
                                bitcodeAdapter = tempFiles.create("api", ".bc").javaFile(),
                                header = outputFiles.cAdapterHeader.javaFile(),
                                def = if (config.target.family == Family.MINGW) outputFiles.cAdapterDef.javaFile() else null,
                        )
                    } else null
                    // TODO: Make this work if we first compile all the fragments and only after that run the link phases.
                    generationStateEngine.compileModule(fragment.irModule, bitcodeFile, cExportFiles)
                    // Split here
                    val dependenciesTrackingResult = generationState.dependenciesTracker.collectResult()
                    val depsFilePath = config.writeSerializedDependencies
                    if (!depsFilePath.isNullOrEmpty()) {
                        depsFilePath.File().writeLines(DependenciesTrackingResult.serialize(dependenciesTrackingResult))
                    }
                    val moduleCompilationOutput = ModuleCompilationOutput(bitcodeFile, dependenciesTrackingResult)
                    compileAndLink(moduleCompilationOutput, outputFiles.mainFileName, outputFiles, tempFiles, isCoverageEnabled = false)
                }
            } finally {
                tempFiles.dispose()
            }
        }

        val fragments = backendEngine.splitIntoFragments(irModule)
        val threadsCount = context.config.threadsCount
        if (threadsCount == 1) {
            fragments.forEach { fragment ->
                runAfterLowerings(fragment, createGenerationStateAndRunLowerings(fragment))
            }
        } else {
            val fragmentsList = fragments.toList()
            if (fragmentsList.size == 1) {
                val fragment = fragmentsList[0]
                runAfterLowerings(fragment, createGenerationStateAndRunLowerings(fragment))
            } else {
                // We'd love to run entire pipeline in parallel, but it's difficult (mainly because of the lowerings,
                // which need cross-file access all the time and it's not easy to overcome this). So, for now,
                // we split the pipeline into two parts - everything before lowerings (including them)
                // which is run sequentially, and everything else which is run in parallel.
                val generationStates = fragmentsList.map { fragment -> createGenerationStateAndRunLowerings(fragment) }
                val executor = Executors.newFixedThreadPool(threadsCount)
                val thrownFromThread = AtomicReference<Throwable?>(null)
                val tasks = fragmentsList.zip(generationStates).map { (fragment, generationState) ->
                    Callable {
                        try {
                            runAfterLowerings(fragment, generationState)
                        } catch (t: Throwable) {
                            thrownFromThread.set(t)
                        }
                    }
                }
                executor.invokeAll(tasks.toList())
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.DAYS)
                thrownFromThread.get()?.let { throw it }
            }
        }
    }
}

internal fun <C : PhaseContext> PhaseEngine<C>.runBitcodeBackend(context: BitcodePostProcessingContext, dependencies: DependenciesTrackingResult) {
    useContext(context) { bitcodeEngine ->
        val tempFiles = createTempFiles(context.config, null)
        val bitcodeFile = tempFiles.create(context.config.shortModuleName ?: "out", ".bc").javaFile()
        val outputPath = context.config.outputPath
        val outputFiles = OutputFiles(outputPath, context.config.target, context.config.produce)
        bitcodeEngine.runBitcodePostProcessing()
        runPhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(context.llvm.module, bitcodeFile))
        val moduleCompilationOutput = ModuleCompilationOutput(bitcodeFile, dependencies)
        compileAndLink(moduleCompilationOutput, outputFiles.mainFileName, outputFiles, tempFiles, isCoverageEnabled = false)
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
            val cacheDeserializationStrategy = CacheDeserializationStrategy.SingleFile(file.path, file.packageFqName.asString())
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
        val bitcodeFile: java.io.File,
        val dependenciesTrackingResult: DependenciesTrackingResult,
)

/**
 * 1. Runs IR lowerings
 * 2. Runs LTO.
 * 3. Translates IR to LLVM IR.
 * 4. Optimizes it.
 * 5. Serializes it to a bitcode file.
 */
internal fun PhaseEngine<NativeGenerationState>.compileModule(module: IrModuleFragment, bitcodeFile: java.io.File, cExportFiles: CExportFiles?) {
    runBackendCodegen(module, cExportFiles)
    val checkExternalCalls = context.config.configuration.getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS)
    if (checkExternalCalls) {
        runPhase(CheckExternalCallsPhase)
    }
    newEngine(context as BitcodePostProcessingContext) { it.runBitcodePostProcessing() }
    if (checkExternalCalls) {
        runPhase(RewriteExternalCallsCheckerGlobals)
    }
    if (context.config.produce.isCache) {
        runPhase(SaveAdditionalCacheInfoPhase)
    }
    runPhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(context.llvm.module, bitcodeFile))
}


internal fun <C : PhaseContext> PhaseEngine<C>.compileAndLink(
        moduleCompilationOutput: ModuleCompilationOutput,
        linkerOutputFile: String,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
        isCoverageEnabled: Boolean,
) {
    val compilationResult = temporaryFiles.create("result", ".o").javaFile()
    runPhase(ObjectFilesPhase, ObjectFilesPhaseInput(moduleCompilationOutput.bitcodeFile, compilationResult))
    val linkerOutputKind = determineLinkerOutput(context)
    val (linkerInput, cacheBinaries) = run {
        val resolvedCacheBinaries = resolveCacheBinaries(context.config.cachedLibraries, moduleCompilationOutput.dependenciesTrackingResult)
        when {
            context.config.produce == CompilerOutputKind.STATIC_CACHE -> {
                compilationResult to ResolvedCacheBinaries(emptyList(), resolvedCacheBinaries.dynamic)
            }
            shouldPerformPreLink(context.config, resolvedCacheBinaries, linkerOutputKind) -> {
                val prelinkResult = temporaryFiles.create("withStaticCaches", ".o").javaFile()
                runPhase(PreLinkCachesPhase, PreLinkCachesInput(listOf(compilationResult), resolvedCacheBinaries, prelinkResult))
                // Static caches are linked into binary, so we don't need to pass them.
                prelinkResult to ResolvedCacheBinaries(emptyList(), resolvedCacheBinaries.dynamic)
            }
            else -> {
                compilationResult to resolvedCacheBinaries
            }
        }
    }
    val linkerPhaseInput = LinkerPhaseInput(
            linkerOutputFile,
            linkerOutputKind,
            listOf(linkerInput.canonicalPath),
            moduleCompilationOutput.dependenciesTrackingResult,
            outputFiles,
            cacheBinaries,
            isCoverageEnabled = isCoverageEnabled
    )
    runPhase(LinkerPhase, linkerPhaseInput)
    if (context.config.produce.isCache) {
        runPhase(FinalizeCachePhase, outputFiles)
    }
}

internal fun PhaseEngine<NativeGenerationState>.lowerModuleWithDependencies(module: IrModuleFragment) {
    runAllLowerings(module)
    val dependenciesToCompile = findDependenciesToCompile()
    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM? (and with lazy top-levels initialization?)
    dependenciesToCompile.reversed().forEach { irModule ->
        runAllLowerings(irModule)
    }
    mergeDependencies(module, dependenciesToCompile)
}

internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment, cExportFiles: CExportFiles?) {
    runCodegen(module)
    val generatedBitcodeFiles = if (context.config.produce.isNativeLibrary) {
        require(cExportFiles != null)
        val input = CExportGenerateApiInput(
                context.context.cAdapterExportedElements!!,
                headerFile = cExportFiles.header,
                defFile = cExportFiles.def,
                cppAdapterFile = cExportFiles.cppAdapter
        )
        runPhase(CExportGenerateApiPhase, input)
        runPhase(CExportCompileAdapterPhase, CExportCompileAdapterInput(cExportFiles.cppAdapter, cExportFiles.bitcodeAdapter))
        listOf(cExportFiles.bitcodeAdapter)
    } else {
        emptyList()
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