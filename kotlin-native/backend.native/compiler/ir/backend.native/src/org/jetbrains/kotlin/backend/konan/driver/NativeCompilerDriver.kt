/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.usingJvmCInteropCallbacks
import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.llvm.generateFrameworkFromCachesBitcode
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.backend.konan.objcexport.OBJC_CACHE_BINARY_OPTIONS
import org.jetbrains.kotlin.backend.konan.objcexport.createObjCFrameworkFromCaches
import org.jetbrains.kotlin.backend.konan.objcexport.hasObjCCacheBinaryOptions
import org.jetbrains.kotlin.backend.konan.util.reportCompilationErrorAndThrow
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.CInterfaceGenerationMode
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.config.konanHome
import org.jetbrains.kotlin.konan.config.verifyBitcode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.usingNativeMemoryAllocator
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

/**
 * Driver orchestrates and connects different parts of the compiler into a complete pipeline.
 */
internal class NativeCompilerDriver(private val performanceManager: PerformanceManager?) {

    fun run(config: NativeSecondStageCompilationConfig, environment: KotlinCoreEnvironment) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks(config.configuration.konanHome) {
                PhaseEngine.startTopLevel(config) { engine ->
                    if (!config.compileFromBitcode.isNullOrEmpty()) produceBinaryFromBitcode(engine, config, config.compileFromBitcode!!)
                    else when (config.produce) {
                        CompilerOutputKind.PROGRAM -> produceBinary(engine, config, environment)
                        CompilerOutputKind.DYNAMIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.STATIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.FRAMEWORK -> produceObjCFramework(engine, config, environment)
                        CompilerOutputKind.LIBRARY -> error("klib is supported only in NativeKlibCliPipeline")
                        CompilerOutputKind.BITCODE -> error("Bitcode output kind is obsolete.")
                        CompilerOutputKind.DYNAMIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.STATIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.HEADER_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.OBJC_CACHE -> produceObjCFramework(engine, config, environment)
                        CompilerOutputKind.TEST_BUNDLE -> produceBundle(engine, config, environment)
                    }
                }
            }
        }
    }

    /**
     * Create an Objective-C framework which is a directory consisting of
     * - Objective-C header
     * - Info.plist
     * - Binary (if -Xomit-framework-binary is not passed).
     *
     * With `-produce objc_cache`, compile the Objective-C export of one library into a cache instead.
     */
    private fun produceObjCFramework(engine: PhaseEngine<NativeBackendPhaseContext>, config: NativeSecondStageCompilationConfig, environment: KotlinCoreEnvironment) {
        if (config.produce == CompilerOutputKind.FRAMEWORK && canProduceObjCFrameworkFromCaches(config, environment) &&
                produceObjCFrameworkFromCaches(engine, config)) {
            return
        }

        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(config, environment) }
                ?: return

        val objCExportedInterface = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            engine.runPhase(ProduceObjCExportInterfacePhase, frontendOutput).also {
                if (config.produce == CompilerOutputKind.FRAMEWORK) {
                    engine.runPhase(CreateObjCFrameworkPhase, CreateObjCFrameworkInput(frontendOutput.moduleDescriptor, it))
                }
            }
        }
        if (config.produce == CompilerOutputKind.FRAMEWORK && config.omitFrameworkBinary) {
            return
        }

        val [linkKlibsOutput, objCCodeSpec] = performanceManager.tryMeasurePhaseTime(PhaseType.IrLinking) {
            engine.linkKlibs(frontendOutput) {
                it.runPhase(CreateObjCExportCodeSpecPhase, objCExportedInterface)
            }
        }

        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, linkKlibsOutput) {
            it.objCExportedInterface = objCExportedInterface
            it.objCExportCodeSpec = objCCodeSpec
        }
        engine.runBackend(backendContext, linkKlibsOutput.irModulesToCompile, performanceManager)
    }

    private fun canProduceObjCFrameworkFromCaches(config: NativeSecondStageCompilationConfig, environment: KotlinCoreEnvironment): Boolean {
        val explicitSetting = config.configuration.get(BinaryOptions.frameworkFromCaches)
        if (explicitSetting == false) return false

        val hasSources = environment.getSourceFiles().isNotEmpty()
        val hasIncludedLibraries = config.loadedKlibs.included.isNotEmpty()
        val hasSelectorToSignatureDump = config.dumpObjcSelectorToSignatureMapping != null
        val exportedLibraries = config.exportedLibrariesForObjCCache
        val uncachedLibraries = config.klibDag.librariesReverseTopoSorted.filterNot { config.cachedLibraries.isLibraryCached(it) }
        val hasObjCCacheBinaryOptions = config.configuration.hasObjCCacheBinaryOptions()
        val [librariesWithIgnoredObjCCache, librariesWithoutObjCCache] = exportedLibraries
                .filterNot { it in config.objCCaches }
                .partition { config.cachedLibraries.whyObjCCacheIsIgnored(it) != null }
        val reasons = buildList {
            if (hasSources) add("source files are present")
            if (hasIncludedLibraries) add("-Xinclude libraries are present")
            if (hasSelectorToSignatureDump) add("dumpObjcSelectorToSignatureMapping is enabled")
            if (config.swiftExport) add("Swift export is enabled")
            if (exportedLibraries.isEmpty()) add("no libraries are exported")
            if (uncachedLibraries.isNotEmpty()) add("[${uncachedLibraries.joinToString { it.uniqueName }}] have no binary cache")
            if (!hasObjCCacheBinaryOptions) {
                add("Objective-C caches require $OBJC_CACHE_BINARY_OPTIONS")
            } else if (!hasSelectorToSignatureDump && !config.swiftExport) {
                if (librariesWithoutObjCCache.isNotEmpty()) {
                    add("[${librariesWithoutObjCCache.joinToString { it.uniqueName }}] have no Objective-C cache")
                }
                librariesWithIgnoredObjCCache.mapTo(this) { config.cachedLibraries.whyObjCCacheIsIgnored(it)!! }
            }
        }
        if (explicitSetting == true && reasons.isNotEmpty()) {
            config.configuration.reportCompilationErrorAndThrow(
                    "Cannot produce the framework from caches (-Xbinary=frameworkFromCaches=true):\n" +
                            reasons.joinToString("\n") { "- $it" }
            )
        }
        return reasons.isEmpty()
    }

    private fun produceObjCFrameworkFromCaches(
            engine: PhaseEngine<NativeBackendPhaseContext>,
            config: NativeSecondStageCompilationConfig,
    ): Boolean {
        val outputFiles = OutputFiles(config.outputPath, config.target, config.produce)
        val created = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            createObjCFrameworkFromCaches(engine.context, outputFiles.mainFile)
        }
        if (!created) return false
        if (config.omitFrameworkBinary) return true

        val llvmModuleSpecification = DefaultLlvmModuleSpecification(config.cachedLibraries)
        val dependenciesTracker = DependenciesTrackerImpl(llvmModuleSpecification, config, context = null, libraryToCache = null)
        val generationState = NativeGenerationState(
                config,
                contextOrNull = null,
                cacheInfo = null,
                dependenciesTracker = dependenciesTracker,
                llvmModuleSpecification = llvmModuleSpecification,
                outputFiles = outputFiles,
                llvmModuleName = "out",
                performanceManager = performanceManager,
        )
        try {
            performanceManager?.notifyPhaseStarted(PhaseType.Backend)
            engine.newEngine(generationState) { generationStateEngine ->
                generateFrameworkFromCachesBitcode(generationState)
                if (config.needCompilerVerification || config.configuration.verifyBitcode) {
                    generationStateEngine.runAndMeasurePhase(VerifyBitcodePhase, generationState.llvm.module)
                }
                if (generationState.shouldPrintBitCode()) {
                    generationStateEngine.runAndMeasurePhase(PrintBitcodePhase, generationState.llvm.module)
                }
                generationStateEngine.runAndMeasurePhase(LinkBitcodeDependenciesPhase, emptyList())
            }
            val dependencies = dependenciesTracker.collectResult()
            config.writeSerializedDependencies?.takeIf { it.isNotEmpty() }?.let {
                Path(it).writeLines(DependenciesTrackingResult.serialize(dependencies))
            }
            engine.runBitcodeBackend(generationState, dependencies)
        } finally {
            performanceManager?.notifyPhaseFinished(PhaseType.Backend)
            generationState.dispose()
        }
        return true
    }

    private fun produceCLibrary(engine: PhaseEngine<NativeBackendPhaseContext>, config: NativeSecondStageCompilationConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(config, environment) }
                ?: return

        // Note: `BuildCExports` is technically not a part of IR linking. Ideally, it should be attributed to `TranslationToIr`,
        // mirroring `ProduceObjCExportInterfacePhase` in `produceObjCFramework`,
        // or both should be moved to a separate dedicated phase type, e.g. `Export`.
        val [linkKlibsOutput, k1CAdapterElements] = performanceManager.tryMeasurePhaseTime(PhaseType.IrLinking) {
            engine.linkKlibs(frontendOutput) {
                if (config.cInterfaceGenerationMode == CInterfaceGenerationMode.V1 && !config.cExportUseIrDiscovery) {
                    it.runPhase(BuildCExports, frontendOutput)
                } else {
                    null
                }
            }
        }

        val cAdapterElements = if (config.cInterfaceGenerationMode == CInterfaceGenerationMode.V1) {
            if (config.cExportUseIrDiscovery) {
                engine.runAndMeasurePhase(BuildCExportsFromIr, linkKlibsOutput)
            } else {
                k1CAdapterElements!!
            }
        } else {
            null
        }
        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, linkKlibsOutput) {
            it.cAdapterExportedElements = cAdapterElements
        }
        engine.runBackend(backendContext, linkKlibsOutput.irModulesToCompile, performanceManager)
    }

    /**
     * Produce a single binary artifact.
     */
    private fun produceBinary(engine: PhaseEngine<NativeBackendPhaseContext>, config: NativeSecondStageCompilationConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(config, environment) }
                ?: return

        val linkKlibsOutput = performanceManager.tryMeasurePhaseTime(PhaseType.IrLinking) { engine.linkKlibs(frontendOutput) }
        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, linkKlibsOutput)
        engine.runBackend(backendContext, linkKlibsOutput.irModulesToCompile, performanceManager)
    }

    private fun produceBinaryFromBitcode(engine: PhaseEngine<NativeBackendPhaseContext>, config: NativeSecondStageCompilationConfig, bitcodeFilePath: String) {
        loadLLVMStubs(config.configuration.konanHome)
        val llvmContext = LLVMContextCreate()!!
        var llvmModule: CPointer<LLVMOpaqueModule>? = null
        try {
            llvmModule = parseBitcodeFile(engine.context, engine.context.diagnosticReporter, llvmContext, bitcodeFilePath)
            val context = BitcodePostProcessingContextImpl(config, llvmModule, llvmContext)
            val depsPath = config.readSerializedDependencies
            val dependencies = if (depsPath.isNullOrEmpty()) DependenciesTrackingResult(emptyList(), emptyList(), emptyList()).also {
                config.configuration.report(CliDiagnostics.KONAN_ARGUMENT_WARNING, "No backend dependencies provided.")
            } else DependenciesTrackingResult.deserialize(depsPath, Path(depsPath).readLines(), config)
            engine.runBitcodeBackend(context, dependencies)
        } finally {
            llvmModule?.let { LLVMDisposeModule(it) }
            LLVMContextDispose(llvmContext)
        }
    }

    /**
     * Produce a bundle that is a directory with code and resources.
     * It consists of
     * - Info.plist
     * - Binary without an entry point.
     *
     * See https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFBundles/AboutBundles/AboutBundles.html
     */
    private fun produceBundle(engine: PhaseEngine<NativeBackendPhaseContext>, config: NativeSecondStageCompilationConfig, environment: KotlinCoreEnvironment) {
        require(config.target.family.isAppleFamily)
        require(config.produce == CompilerOutputKind.TEST_BUNDLE)

        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(config, environment) }
                ?: return
        performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) { engine.runPhase(CreateTestBundlePhase, frontendOutput.moduleDescriptor) }
        val linkKlibsOutput = performanceManager.tryMeasurePhaseTime(PhaseType.IrLinking) { engine.linkKlibs(frontendOutput) }
        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, linkKlibsOutput)
        engine.runBackend(backendContext, linkKlibsOutput.irModulesToCompile, performanceManager)
    }

    private fun createBackendContext(
            config: NativeSecondStageCompilationConfig,
            moduleDescriptor: ModuleDescriptor,
            linkKlibsOutput: LinkKlibsOutput,
            additionalDataSetter: (NativeBackendContext) -> Unit = {}
    ) = NativeBackendContext(
            config,
            moduleDescriptor.builtIns,
            linkKlibsOutput.irBuiltIns,
            linkKlibsOutput.irModules,
            linkKlibsOutput.irLinker,
            linkKlibsOutput.symbols,
            linkKlibsOutput.symbolTable,
    ).also {
        additionalDataSetter(it)
    }
}