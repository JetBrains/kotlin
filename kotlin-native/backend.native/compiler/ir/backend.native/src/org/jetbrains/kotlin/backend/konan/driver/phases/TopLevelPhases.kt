/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.utilities.CExportFiles
import org.jetbrains.kotlin.backend.konan.driver.utilities.createTempFiles
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
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

internal fun <C : PhaseContext> PhaseEngine<C>.runBackend(backendContext: Context, irModule: IrModuleFragment) {
    val config = context.config
    val rootPerformanceManager = backendContext.configuration.performanceManager
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
                        if (context.config.produce.isHeaderCache) return@newEngine
                    }
                    if (context.config.produce == CompilerOutputKind.PROGRAM) {
                        generationStateEngine.runPhase(EntryPointPhase, module)
                    }
                    rootPerformanceManager.trackIRLowering {
                        generationStateEngine.lowerModuleWithDependencies(module)
                    }
                }
                return generationState
            } catch (t: Throwable) {
                generationState.dispose()
                throw t
            }
        }

        data class SubFragment(
                val name: String,
                val files: List<IrFile>,
                val module: IrModuleFragment,
        )

        val FINAL_SUB_FRAGMENT_NAME = "__FINAL__"

        fun SubFragment.generationState(topLevel: NativeGenerationState): NativeGenerationState {
            val llvmModuleSpecification = if (topLevel.llvmModuleSpecification is DefaultLlvmModuleSpecification) {
                object : LlvmModuleSpecificationBase(config.cachedLibraries) {
                    override val isFinal: Boolean
                        get() = name == FINAL_SUB_FRAGMENT_NAME

                    override fun containsLibrary(library: KotlinLibrary): Boolean {
                        if (cachedLibraries.isLibraryCached(library))
                            return false
                        if (name == "")
                            return true
                        if (library.isCInteropLibrary() && name == FINAL_SUB_FRAGMENT_NAME) {
                            return true
                        }
                        return name == library.uniqueName
                    }
                }
            } else topLevel.llvmModuleSpecification
            return topLevel.createChild(topLevel.llvmModuleName + name, llvmModuleSpecification).apply {
                val containsStdlib = name == "" || name == topLevel.context.stdlibModule.konanLibrary!!.uniqueName
                if (containsStdlib && cacheDeserializationStrategy.containsRuntime) {
                    files.filter { isReferencedByNativeRuntime(it.declarations) }
                            .forEach { dependenciesTracker.add(it) }
                }
                if (name == "" || name == FINAL_SUB_FRAGMENT_NAME) {
                    dependenciesTracker.setParent(topLevel.dependenciesTracker)
                }
            }
        }

        fun splitFragment(fragment: BackendJobFragment): List<SubFragment> {
            if (!context.shouldOptimize()) {
                return listOf(SubFragment("", fragment.irModule.files, fragment.irModule))
            }
            return buildList {
                val perLibraryFiles = fragment.irModule.files.groupBy {
                    val library = it.konanLibrary!!
                    if (library.isCInteropLibrary()) FINAL_SUB_FRAGMENT_NAME else library.uniqueName
                }
                perLibraryFiles.mapNotNullTo(this) { (name, files) ->
                    SubFragment(name, files, fragment.irModule).takeIf { name != FINAL_SUB_FRAGMENT_NAME }
                }
                add(SubFragment(FINAL_SUB_FRAGMENT_NAME, perLibraryFiles[FINAL_SUB_FRAGMENT_NAME].orEmpty(), fragment.irModule))
            }
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
                fragment.performanceManager?.notifyIRGenerationStarted()
                newEngine(generationState) { generationStateEngine ->
                    generationStateEngine.runGlobalOptimizations(fragment.irModule)
                }
                val subfragments = splitFragment(fragment)

                val moduleCompilationOutputs = subfragments.map {
                    backendEngine.useContext(it.generationState(generationState)) { generationStateEngine ->
                        val bitcodeFile = tempFiles.create(generationStateEngine.context.llvmModuleName, "${it.name}.bc").javaFile()
                        val objectFile = tempFiles.create(File(outputFiles.nativeBinaryFile).name, "${it.name}.o").javaFile()
                        val cExportFiles = if (config.produceCInterface) {
                            CExportFiles(
                                    cppAdapter = tempFiles.create("api", ".cpp").javaFile(),
                                    bitcodeAdapter = tempFiles.create("api", ".bc").javaFile(),
                                    header = outputFiles.cAdapterHeader.javaFile(),
                                    def = if (config.target.family == Family.MINGW) outputFiles.cAdapterDef.javaFile() else null,
                            )
                        } else null
                        // TODO: Make this work if we first compile all the fragments and only after that run the link phases.
                        generationStateEngine.compileModule(it.module, it.files, backendContext.irBuiltIns, bitcodeFile, objectFile, cExportFiles)
                        ModuleCompilationOutput(listOf(objectFile), generationStateEngine.context.dependenciesTracker.collectResult())
                    }
                }

                val dependencies = DependenciesTrackingResult.merge(moduleCompilationOutputs.map { it.dependenciesTrackingResult }, context.config)
                val objectFiles = buildList {
                    moduleCompilationOutputs.flatMapTo(this) { it.objectFiles }
                }
                val moduleCompilationOutput = ModuleCompilationOutput(objectFiles, dependencies)

                generationState.dependenciesTracker.collectResult().let { topLevelResult ->
                    topLevelResult.nativeDependenciesToLink.forEach {
                        check(moduleCompilationOutput.dependenciesTrackingResult.nativeDependenciesToLink.contains(it)) {
                            "${it.uniqueName} not found in nativeDependenciesToLink: ${moduleCompilationOutput.dependenciesTrackingResult.nativeDependenciesToLink.map { it.uniqueName }}"
                        }
                    }
                    topLevelResult.allNativeDependencies.forEach {
                        check(moduleCompilationOutput.dependenciesTrackingResult.allNativeDependencies.contains(it)) {
                            "${it.uniqueName} not found in allNativeDependencies: ${moduleCompilationOutput.dependenciesTrackingResult.allNativeDependencies.map { it.uniqueName }}"
                        }
                    }
                    topLevelResult.allCachedBitcodeDependencies.forEach {
                        check(moduleCompilationOutput.dependenciesTrackingResult.allCachedBitcodeDependencies.any { it2 ->
                            it2.library.uniqueName == it.library.uniqueName
                        }) {
                            "${it.library.uniqueName} not found in allCachedBitcodeDependencies: ${moduleCompilationOutput.dependenciesTrackingResult.allCachedBitcodeDependencies.map { it.library.uniqueName }}"
                        }
                    }
                }

                val depsFilePath = config.writeSerializedDependencies
                if (!depsFilePath.isNullOrEmpty()) {
                    depsFilePath.File().writeLines(DependenciesTrackingResult.serialize(moduleCompilationOutput.dependenciesTrackingResult))
                }
                linkBinary(moduleCompilationOutput, outputFiles.mainFileName, outputFiles, tempFiles)
            } finally {
                tempFiles.dispose()
                fragment.performanceManager?.notifyIRGenerationFinished()
            }
        }

        val fragments = backendEngine.splitIntoFragments(irModule)
        val threadsCount = context.config.threadsCount
        if (threadsCount == 1) {
            val fragmentsList = fragments.toList()
            val generationStates = fragmentsList.map { fragment -> createGenerationStateAndRunLowerings(fragment) }
            fragmentsList.zip(generationStates).forEach { (fragment, generationState) ->
                runAfterLowerings(fragment, generationState)
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
        (rootPerformanceManager as? K2NativeCompilerPerformanceManager)?.collectChildMeasurements()
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
        val objectFile = tempFiles.create(File(outputFiles.nativeBinaryFile).name, ".o").javaFile()
        runPhase(ObjectFilesPhase, ObjectFilesPhaseInput(bitcodeFile, objectFile))
        val moduleCompilationOutput = ModuleCompilationOutput(listOf(objectFile), dependencies)
        linkBinary(moduleCompilationOutput, outputFiles.mainFileName, outputFiles, tempFiles)
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
        val performanceManager: CommonCompilerPerformanceManager?,
)

private fun PhaseEngine<out Context>.splitIntoFragments(
        input: IrModuleFragment,
): Sequence<BackendJobFragment> {
    val config = context.config
    val performanceManager = config.configuration.performanceManager as? K2NativeCompilerPerformanceManager
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
                    performanceManager?.createChild(),
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
                        performanceManager?.createChild(),
                )
        )
    }
}

internal data class ModuleCompilationOutput(
        val objectFiles: List<java.io.File>,
        val dependenciesTrackingResult: DependenciesTrackingResult,
)

/**
 * 1. Translates IR to LLVM IR.
 * 2. Optimizes it.
 * 3. Serializes it to a bitcode file.
 */
internal fun PhaseEngine<NativeGenerationState>.compileModule(
        module: IrModuleFragment,
        files: List<IrFile>,
        irBuiltIns: IrBuiltIns,
        bitcodeFile: java.io.File,
        objectFile: java.io.File,
        cExportFiles: CExportFiles?
) {
    runBackendCodegen(module, files, irBuiltIns, cExportFiles)
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
    runPhase(ObjectFilesPhase, ObjectFilesPhaseInput(bitcodeFile, objectFile))
}

internal fun <C : PhaseContext> PhaseEngine<C>.linkBinary(
        moduleCompilationOutput: ModuleCompilationOutput,
        linkerOutputFile: String,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
) {
    val linkerOutputKind = determineLinkerOutput(context)
    val (linkerInput, cacheBinaries) = run {
        val resolvedCacheBinaries by lazy { resolveCacheBinaries(context.config.cachedLibraries, moduleCompilationOutput.dependenciesTrackingResult) }
        when {
            context.config.produce == CompilerOutputKind.STATIC_CACHE -> {
                moduleCompilationOutput.objectFiles to ResolvedCacheBinaries(emptyList(), emptyList())
            }
            shouldPerformPreLink(context.config, resolvedCacheBinaries, linkerOutputKind) -> {
                val prelinkResult = temporaryFiles.create("withStaticCaches", ".o").javaFile()
                runPhase(PreLinkCachesPhase, PreLinkCachesInput(moduleCompilationOutput.objectFiles, resolvedCacheBinaries, prelinkResult))
                // Static caches are linked into binary, so we don't need to pass them.
                listOf(prelinkResult) to ResolvedCacheBinaries(emptyList(), resolvedCacheBinaries.dynamic)
            }
            else -> {
                moduleCompilationOutput.objectFiles to resolvedCacheBinaries
            }
        }
    }
    val linkerPhaseInput = LinkerPhaseInput(
            linkerOutputFile,
            linkerOutputKind,
            linkerInput.map { it.canonicalPath },
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

internal fun PhaseEngine<NativeGenerationState>.lowerModuleWithDependencies(module: IrModuleFragment) {
    val dependenciesToCompile = findDependenciesToCompile()
    // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
    // TODO: Does the order of files really matter with the new MM? (and with lazy top-levels initialization?)
    val allModulesToLower = listOf(module) + dependenciesToCompile.reversed()

    // In Kotlin/Native, lowerings are run not over modules, but over individual files.
    // This means that there is no guarantee that after running a lowering in file A, the same lowering has already been run in file B,
    // and vice versa.
    // However, in order to validate IR after inlining, we have to make sure that all the modules being compiled are lowered to the same
    // stage, because otherwise we may be actually validating a partially lowered IR that may not pass certain checks
    // (like IR visibility checks).
    // This is what we call a 'lowering synchronization point'.
    runModuleWisePhase(validateIrBeforeLowering, allModulesToLower)
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
        runLowerings(getLoweringsUpToAndIncludingSyntheticAccessors(), allModulesToLower)
        if (!context.config.configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING)) {
            runModuleWisePhase(validateIrAfterInliningOnlyPrivateFunctions, allModulesToLower)
            if (context.config.configuration[KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR] != null) {
                runModuleWisePhase(dumpSyntheticAccessorsPhase, allModulesToLower)
            }
        }
        runLowerings(listOf(inlineAllFunctionsPhase), allModulesToLower)
    }
    runModuleWisePhase(validateIrAfterInliningAllFunctions, allModulesToLower)
    runLowerings(getLoweringsAfterInlining(), allModulesToLower)
    runModuleWisePhase(validateIrAfterLowering, allModulesToLower)

    mergeDependencies(module, dependenciesToCompile)
}

internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment, files: List<IrFile>, irBuiltIns: IrBuiltIns, cExportFiles: CExportFiles?) {
    runCodegen(module, files, irBuiltIns)
    val generatedBitcodeFiles = if (context.config.produceCInterface && (!context.shouldOptimize() || context.llvmModuleSpecification.isFinal)) {
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
    if (!context.shouldOptimize() || context.llvmModuleSpecification.isFinal) {
        runPhase(CStubsPhase)
    }
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

private fun PhaseEngine<NativeGenerationState>.runGlobalOptimizations(module: IrModuleFragment) {
    val optimize = false // context.shouldOptimize()
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
    context.dceResult = runPhase(DCEPhase, DCEInput(module, moduleDFG), disable = !optimize)
    module.files.forEach {
        runPhase(CoroutinesVarSpillingPhase, it)
    }
    runPhase(GHAPhase, module, disable = !optimize)
    context.lifetimes = runPhase(EscapeAnalysisPhase, EscapeAnalysisInput(module, moduleDFG), disable = !optimize)
}

/**
 * Compile lowered [module] to object file.
 * @return absolute path to object file.
 */
private fun PhaseEngine<NativeGenerationState>.runCodegen(module: IrModuleFragment, files: List<IrFile>, irBuiltIns: IrBuiltIns) {
    runPhase(CreateLLVMDeclarationsPhase, files)
    runPhase(RTTIPhase, RTTIInput(files, context.dceResult))
    runPhase(CodegenPhase, CodegenInput(module, files, irBuiltIns, context.lifetimes))
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
