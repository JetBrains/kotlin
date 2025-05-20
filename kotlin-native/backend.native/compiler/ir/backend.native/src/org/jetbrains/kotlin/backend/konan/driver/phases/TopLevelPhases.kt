/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.CExportFiles
import org.jetbrains.kotlin.backend.konan.driver.utilities.createTempFiles
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.serialization.PartialCacheInfo
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal fun PhaseEngine<PhaseContext>.runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
    val languageVersion = config.languageVersionSettings.languageVersion
    val kotlinSourceRoots = environment.configuration.kotlinSourceRoots
    if (languageVersion.usesK2 && kotlinSourceRoots.isNotEmpty()) {
        throw Error("Attempt to run K1 from unsupported LV=${languageVersion}")
    }

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
        psiToIrEngine.runSpecialBackendChecks(output.irModule, output.irBuiltIns, output.symbols)
        output to additionalOutput
    }
    runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput)
    return psiToIrOutput to additionalOutput
}

internal fun <C : PhaseContext> PhaseEngine<C>.runBackend(backendContext: Context, irModule: IrModuleFragment, performanceManager: PerformanceManager?) {
    val config = context.config
    useContext(backendContext) { backendEngine ->
        backendEngine.runPhase(functionsWithoutBoundCheck)

        fun createGenerationState(fragment: BackendJobFragment): NativeGenerationState {
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
                        if (context.config.produce.isHeaderCache) return@newEngine
                    }
                    if (context.config.produce == CompilerOutputKind.PROGRAM) {
                        generationStateEngine.runPhase(EntryPointPhase, module)
                    }
                }
                return generationState
            } catch (t: Throwable) {
                generationState.dispose()
                throw t
            }
        }

        fun NativeGenerationState.runEngineForLowerings(block: PhaseEngine<NativeGenerationState>.() -> Unit) {
            try {
                newEngine(this) { generationStateEngine ->
                    generationStateEngine.block()
                }
            } catch (t: Throwable) {
                this.dispose()
                throw t
            }
        }

        fun NativeGenerationState.runSpecifiedLowerings(fragment: BackendJobFragment, loweringsToLaunch: LoweringList) {
            runEngineForLowerings {
                val module = fragment.irModule
                partiallyLowerModuleWithDependencies(module, loweringsToLaunch)
            }
        }

        fun NativeGenerationState.runSpecifiedLowerings(fragment: BackendJobFragment, moduleLowering: ModuleLowering) {
            runEngineForLowerings {
                val module = fragment.irModule
                partiallyLowerModuleWithDependencies(module, moduleLowering)
            }
        }

        fun NativeGenerationState.finalizeLowerings(fragment: BackendJobFragment) {
            runEngineForLowerings {
                val module = fragment.irModule
                val dependenciesToCompile = findDependenciesToCompile()
                mergeDependencies(module, dependenciesToCompile)
            }
        }

        fun List<BackendJobFragment>.runAllLowerings(): List<NativeGenerationState> {
            val generationStates = this.map { fragment -> createGenerationState(fragment) }
            val fragmentWithState = this.zip(generationStates)

            // In Kotlin/Native, lowerings are run not over modules, but over individual files.
            // This means that there is no guarantee that after running a lowering in file A, the same lowering has already been run in file B,
            // and vice versa.
            // However, in order to validate IR after inlining, we have to make sure that all the modules being compiled are lowered to the same
            // stage, because otherwise we may be actually validating a partially lowered IR that may not pass certain checks
            // (like IR visibility checks).
            // This is what we call a 'lowering synchronization point'.
            fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, validateIrBeforeLowering) }
            if (context.config.configuration[KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR] != null) {
                fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, dumpSyntheticAccessorsPhase) }
            }

            run {
                // This is a so-called "KLIB Common Lowerings Prefix".
                //
                // Note: All lowerings up to but excluding "InlineAllFunctions" are supposed to modify only the lowered file.
                // By contrast, "InlineAllFunctions" may mutate multiple files at the same time, and some files can be even
                // mutated several times by little pieces. Which is a completely different behavior as compared to other lowerings.
                // "InlineAllFunctions" expects that for an inlined function all preceding lowerings (including generation of
                // synthetic accessors) have been already applied.
                // To avoid overcomplicating things and to keep running the preceding lowerings with "modify-only-lowered-file"
                // invariant, we would like to put a synchronization point immediately before "InlineAllFunctions".
                fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, getLoweringsUpToAndIncludingSyntheticAccessors()) }
                fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, validateIrAfterInliningOnlyPrivateFunctions) }
                fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, listOf(inlineAllFunctionsPhase)) }
                fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, listOf(specialObjCValidationPhase)) }
            }

            fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, validateIrAfterInliningAllFunctions) }
            fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, listOf(constEvaluationPhase)) }
            fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, state.context.config.getLoweringsAfterInlining()) }
            fragmentWithState.forEach { (fragment, state) -> state.runSpecifiedLowerings(fragment, validateIrAfterLowering) }

            fragmentWithState.forEach { (fragment, state) -> state.finalizeLowerings(fragment) }

            return generationStates
        }

        fun runAfterLowerings(fragment: BackendJobFragment, generationState: NativeGenerationState) {
            val tempFiles = createTempFiles(config, fragment.cacheDeserializationStrategy)
            val outputFiles = generationState.outputFiles
            if (context.config.produce.isHeaderCache) {
                newEngine(generationState) { generationStateEngine ->
                    generationStateEngine.runPhase(SaveAdditionalCacheInfoPhase)
                    File(outputFiles.nativeBinaryFile).createNew()
                    generationStateEngine.runPhase(FinalizeCachePhase, outputFiles)
                }
                return
            }
            try {
                fragment.performanceManager?.notifyPhaseStarted(PhaseType.Backend)
                backendEngine.useContext(generationState) { generationStateEngine ->
                    val bitcodeFile = tempFiles.create(generationState.llvmModuleName, ".bc").javaFile()
                    val cExportFiles = if (config.produceCInterface) {
                        CExportFiles(
                                cppAdapter = tempFiles.create("api", ".cpp").javaFile(),
                                bitcodeAdapter = tempFiles.create("api", ".bc").javaFile(),
                                header = outputFiles.cAdapterHeader.javaFile(),
                                def = if (config.target.family == Family.MINGW) outputFiles.cAdapterDef.javaFile() else null,
                        )
                    } else null
                    // TODO: Make this work if we first compile all the fragments and only after that run the link phases.
                    generationStateEngine.compileModule(fragment.irModule, backendContext.irBuiltIns, bitcodeFile, cExportFiles)
                    // Split here
                    val dependenciesTrackingResult = generationState.dependenciesTracker.collectResult()
                    val depsFilePath = config.writeSerializedDependencies
                    if (!depsFilePath.isNullOrEmpty()) {
                        depsFilePath.File().writeLines(DependenciesTrackingResult.serialize(dependenciesTrackingResult))
                    }
                    val moduleCompilationOutput = ModuleCompilationOutput(bitcodeFile, dependenciesTrackingResult)
                    compileAndLink(moduleCompilationOutput, outputFiles.mainFileName, outputFiles, tempFiles)
                }
            } finally {
                tempFiles.dispose()
                fragment.performanceManager?.notifyPhaseFinished(PhaseType.Backend)
            }
        }

        val fragments = backendEngine.splitIntoFragments(irModule, performanceManager)
        val fragmentsList = fragments.toList()
        val generationStates = performanceManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            fragmentsList.runAllLowerings()
        }

        val threadsCount = context.config.threadsCount
        if (threadsCount == 1) {
            fragmentsList.zip(generationStates).forEach { (fragment, generationState) ->
                runAfterLowerings(fragment, generationState)
            }
        } else {
            if (fragmentsList.size == 1) {
                runAfterLowerings(fragmentsList.first(), generationStates.first())
            } else {
                // We'd love to run entire pipeline in parallel, but it's difficult (mainly because of the lowerings,
                // which need cross-file access all the time and it's not easy to overcome this). So, for now,
                // we split the pipeline into two parts - everything before lowerings (including them)
                // which is run sequentially, and everything else which is run in parallel.
                val executor = Executors.newFixedThreadPool(threadsCount)
                val thrownFromThread = AtomicReference<Throwable?>(null)
                val tasks = fragmentsList.zip(generationStates).map { (fragment, generationState) ->
                    Callable {
                        try {
                            // Currently, it's not possible to initialize the correct thread on `PerformanceManager` creation
                            // because new threads are spawned here when `fragment` with its `PerformanceManager` is already initialized.
                            fragment.performanceManager?.initializeCurrentThread()
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

        if (performanceManager != null) {
            fragments.forEach {
                performanceManager.addOtherUnitStats(it.performanceManager?.unitStats)
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
        compileAndLink(moduleCompilationOutput, outputFiles.mainFileName, outputFiles, tempFiles)
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
        val performanceManager: PerformanceManager?,
)

private fun PhaseEngine<out Context>.splitIntoFragments(
        input: IrModuleFragment,
        mainPerfManager: PerformanceManager?,
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
            val fragment = IrModuleFragmentImpl(input.descriptor)
            fragment.files += file
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
                    PerformanceManagerImpl.createAndEnableChildIfNeeded(mainPerfManager)?.also { it.notifyPhaseFinished(PhaseType.Initialization) },
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
                        llvmModuleSpecification,
                        PerformanceManagerImpl.createAndEnableChildIfNeeded(mainPerfManager)?.also { it.notifyPhaseFinished(PhaseType.Initialization) },
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
internal fun PhaseEngine<NativeGenerationState>.compileModule(
        module: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        bitcodeFile: java.io.File,
        cExportFiles: CExportFiles?
) {
    runBackendCodegen(module, irBuiltIns, cExportFiles)
    val checkExternalCalls = context.config.checkStateAtExternalCalls
    if (checkExternalCalls) {
        runPhase(CheckExternalCallsPhase)
    }
    newEngine(context as BitcodePostProcessingContext) { it.runBitcodePostProcessing() }
    if (checkExternalCalls) {
        runPhase(RewriteExternalCallsCheckerGlobals)
    }
    if (context.config.produce.isFullCache) {
        runPhase(SaveAdditionalCacheInfoPhase)
    }
    runPhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(context.llvm.module, bitcodeFile))
}


internal fun <C : PhaseContext> PhaseEngine<C>.compileAndLink(
        moduleCompilationOutput: ModuleCompilationOutput,
        linkerOutputFile: String,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
) {
    val compilationResult = temporaryFiles.create(File(outputFiles.nativeBinaryFile).name, ".o").javaFile()
    runPhase(ObjectFilesPhase, ObjectFilesPhaseInput(moduleCompilationOutput.bitcodeFile, compilationResult))
    val linkerOutputKind = determineLinkerOutput(context)
    val (linkerInput, cacheBinaries) = run {
        val resolvedCacheBinaries by lazy { resolveCacheBinaries(context.config.cachedLibraries, moduleCompilationOutput.dependenciesTrackingResult) }
        when {
            context.config.produce == CompilerOutputKind.STATIC_CACHE -> {
                compilationResult to ResolvedCacheBinaries(emptyList(), emptyList())
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
            temporaryFiles,
            cacheBinaries,
    )
    runPhase(LinkerPhase, linkerPhaseInput)
    if (context.config.produce.isCache) {
        runPhase(FinalizeCachePhase, outputFiles)
    }
}

internal fun PhaseEngine<NativeGenerationState>.partiallyLowerModuleWithDependencies(module: IrModuleFragment, loweringList: LoweringList) {
    val dependenciesToCompile = findDependenciesToCompile()
    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM? (and with lazy top-levels initialization?)
    val allModulesToLower = listOf(module) + dependenciesToCompile.reversed()

    runLowerings(loweringList, allModulesToLower)
}

internal fun PhaseEngine<NativeGenerationState>.partiallyLowerModuleWithDependencies(module: IrModuleFragment, lowering: ModuleLowering) {
    val dependenciesToCompile = findDependenciesToCompile()
    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM? (and with lazy top-levels initialization?)
    val allModulesToLower = listOf(module) + dependenciesToCompile.reversed()

    runModuleWisePhase(lowering, allModulesToLower)
}

internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment, irBuiltIns: IrBuiltIns, cExportFiles: CExportFiles?) {
    runCodegen(module, irBuiltIns)
    val generatedBitcodeFiles = if (context.config.produceCInterface) {
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
private fun PhaseEngine<NativeGenerationState>.runCodegen(module: IrModuleFragment, irBuiltIns: IrBuiltIns) {
    val optimize = context.shouldOptimize()
    val enablePreCodegenInliner = context.config.preCodegenInlineThreshold != 0U && optimize
    module.files.forEach {
        runPhase(ReturnsInsertionPhase, it)
        // Have to run after link dependencies phase, because fields from dependencies can be changed during lowerings.
        // Inline accessors only in optimized builds due to separate compilation and possibility to get broken debug information.
        runPhase(PropertyAccessorInlinePhase, it, disable = !optimize)
        runPhase(InlineClassPropertyAccessorsPhase, it, disable = !optimize)
    }
    val moduleDFG = runPhase(BuildDFGPhase, module, disable = !optimize)
    runPhase(RemoveRedundantCallsToStaticInitializersPhase, RedundantCallsInput(moduleDFG, module), disable = !enablePreCodegenInliner)
    runPhase(PreCodegenInlinerPhase, PreCodegenInlinerInput(module, moduleDFG), disable = !enablePreCodegenInliner)
    runPhase(DevirtualizationAnalysisPhase, DevirtualizationAnalysisInput(module, moduleDFG), disable = !optimize)
    // KT-72336: This is more optimal but contradicts with the pre-codegen inliner.
    runPhase(RemoveRedundantCallsToStaticInitializersPhase, RedundantCallsInput(moduleDFG, module), disable = enablePreCodegenInliner || !optimize)
    runPhase(DevirtualizationPhase, DevirtualizationInput(module, moduleDFG), disable = !optimize)
    module.files.forEach {
        runPhase(RedundantCoercionsCleaningPhase, it)
        // depends on redundantCoercionsCleaningPhase
        runPhase(UnboxInlinePhase, it, disable = !optimize)
    }
    runPhase(PreCodegenInlinerPhase, PreCodegenInlinerInput(module, moduleDFG), disable = !enablePreCodegenInliner)
    val dceResult = runPhase(DCEPhase, DCEInput(module, moduleDFG), disable = !optimize)
    module.files.forEach {
        runPhase(CoroutinesVarSpillingPhase, it)
    }
    runPhase(CreateLLVMDeclarationsPhase, module)
    runPhase(GHAPhase, module, disable = !optimize)
    runPhase(RTTIPhase, RTTIInput(module, dceResult))
    val lifetimes = runPhase(EscapeAnalysisPhase, EscapeAnalysisInput(module, moduleDFG), disable = !optimize)
    runPhase(CodegenPhase, CodegenInput(module, irBuiltIns, lifetimes))
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
