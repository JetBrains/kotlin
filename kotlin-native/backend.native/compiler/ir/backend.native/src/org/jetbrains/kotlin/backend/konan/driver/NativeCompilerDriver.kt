/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import com.intellij.openapi.project.Project
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.usingJvmCInteropCallbacks
import llvm.LLVMContextCreate
import llvm.LLVMContextDispose
import llvm.LLVMDisposeModule
import llvm.LLVMOpaqueModule
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.usingNativeMemoryAllocator

/**
 * Driver orchestrates and connects different parts of the compiler into a complete pipeline.
 */
internal class NativeCompilerDriver(private val performanceManager: PerformanceManager?) {

    fun run(config: BaseNativeConfig, environment: KotlinCoreEnvironment, project: Project) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                PhaseEngine.startTopLevel(config.configuration) { engine ->
                    val compileFromBitcode = config.configuration.get(KonanConfigKeys.COMPILE_FROM_BITCODE)
                    if (!compileFromBitcode.isNullOrEmpty()) produceBinaryFromBitcode(engine, config, compileFromBitcode)
                    else when (config.configuration.get(KonanConfigKeys.PRODUCE)!!) {
                        CompilerOutputKind.PROGRAM -> produceBinary(engine, config, environment, project)
                        CompilerOutputKind.DYNAMIC -> produceCLibrary(engine, config, environment, project)
                        CompilerOutputKind.STATIC -> produceCLibrary(engine, config, environment, project)
                        CompilerOutputKind.FRAMEWORK -> produceObjCFramework(engine, config, environment, project)
                        CompilerOutputKind.LIBRARY -> produceKlib(engine, config, environment, project)
                        CompilerOutputKind.BITCODE -> error("Bitcode output kind is obsolete.")
                        CompilerOutputKind.DYNAMIC_CACHE -> produceBinary(engine, config, environment, project)
                        CompilerOutputKind.STATIC_CACHE -> produceBinary(engine, config, environment, project)
                        CompilerOutputKind.HEADER_CACHE -> produceBinary(engine, config, environment, project)
                        CompilerOutputKind.TEST_BUNDLE -> produceBundle(engine, config, environment, project)
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
     */
    private fun produceObjCFramework(
            engine: PhaseEngine<PhaseContext>,
            config: BaseNativeConfig,
            environment: KotlinCoreEnvironment,
            project: Project
    ) {
        val frontendOutput = engine.startFrontendEngine(config) { engine ->
            performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(environment, project) }
        } ?: return

        engine.startBackendEngine(config) { engine ->
            val (objCExportedInterface, psiToIrOutput, objCCodeSpec) = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
                val objCExportedInterface = engine.runPhase(ProduceObjCExportInterfacePhase, frontendOutput)
                engine.runPhase(CreateObjCFrameworkPhase, CreateObjCFrameworkInput(frontendOutput.moduleDescriptor, objCExportedInterface))
                val (psiToIrOutput, objCCodeSpec) = engine.runIrLinker(frontendOutput) {
                    it.runPhase(CreateObjCExportCodeSpecPhase, objCExportedInterface)
                }
                if (engine.context.config.omitFrameworkBinary) {
                    return
                }
                Triple(objCExportedInterface, psiToIrOutput, objCCodeSpec)
            }

            val backendContext = createBackendContext(engine.context.config, frontendOutput, psiToIrOutput) {
                it.objCExportedInterface = objCExportedInterface
                it.objCExportCodeSpec = objCCodeSpec
            }
            engine.runBackend(backendContext, psiToIrOutput.irModule, performanceManager)
        }
    }

    private fun produceCLibrary(
            engine: PhaseEngine<PhaseContext>,
            config: BaseNativeConfig,
            environment: KotlinCoreEnvironment,
            project: Project,
    ) {
        val frontendOutput = engine.startFrontendEngine(config) { engine ->
            performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(environment, project) }
        } ?: return

        engine.startBackendEngine(config) { engine ->
            val (psiToIrOutput, cAdapterElements) = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
                engine.runIrLinker(frontendOutput) {
                    if (engine.context.config.cInterfaceGenerationMode == CInterfaceGenerationMode.V1) {
                        it.runPhase(BuildCExports, frontendOutput)
                    } else {
                        null
                    }
                }
            }

            val backendContext = createBackendContext(engine.context.config, frontendOutput, psiToIrOutput) {
                it.cAdapterExportedElements = cAdapterElements
            }
            engine.runBackend(backendContext, psiToIrOutput.irModule, performanceManager)
        }
    }

    private fun produceKlib(
            engine: PhaseEngine<PhaseContext>,
            config: BaseNativeConfig,
            environment: KotlinCoreEnvironment,
            project: Project,
    ) {
        engine.useContext(FrontendContextImpl(NativeFrontendConfig(config))) { engine ->
            val serializerOutput = if (environment.configuration.getBoolean(CommonConfigurationKeys.USE_FIR))
                serializeKLibK2(engine, engine.context.config, environment, project)
            else
                serializeKlibK1(engine, engine.context.config, environment, project = project)
            serializerOutput?.let {
                performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                    engine.writeKlib(it)
                }
            }
        }
    }

    private fun serializeKLibK2(
            engine: PhaseEngine<out FrontendContext>,
            config: NativeFrontendConfig,
            environment: KotlinCoreEnvironment,
            project: Project,
    ): SerializerOutput? {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFirFrontend(environment) }
        if (frontendOutput is FirOutput.ShouldNotGenerateCode) return null
        require(frontendOutput is FirOutput.Full)

        return if (config.metadataKlib) {
            performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                engine.runFirSerializer(frontendOutput)
            }
        } else {
            val fir2IrOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
                engine.runFir2Ir(Fir2IrInput(frontendOutput, project, config.resolvedLibraries))
            }
            val headerKlibPath = config.headerKlibPath
            if (!headerKlibPath.isNullOrEmpty()) {
                val headerKlib = performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                    engine.runFir2IrSerializer(FirSerializerInput(fir2IrOutput, produceHeaderKlib = true))
                }
                performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                    engine.writeKlib(headerKlib, headerKlibPath, produceHeaderKlib = true)
                }
                // Don't overwrite the header klib with the full klib and stop compilation here.
                // By providing the same path for both regular output and header klib we can skip emitting the full klib.
                if (File(config.outputPath).canonicalPath == File(headerKlibPath).canonicalPath) return null
            }

            engine.runSpecialBackendChecks(SpecialBackendChecksInput(
                    fir2IrOutput.fir2irActualizedResult.irModuleFragment,
                    fir2IrOutput.fir2irActualizedResult.irBuiltIns,
                    fir2IrOutput.symbols,
                    config.target)
            )

            val loweredIr = performanceManager.tryMeasurePhaseTime(PhaseType.IrPreLowering) {
                engine.runPreSerializationLowerings(fir2IrOutput, environment)
            }
            performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                engine.runFir2IrSerializer(FirSerializerInput(loweredIr))
            }
        }
    }

    private fun serializeKlibK1(
            engine: PhaseEngine<out FrontendContext>,
            config: NativeFrontendConfig,
            environment: KotlinCoreEnvironment,
            project: Project,
    ): SerializerOutput? {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(environment, project) }
                ?: return null
        val psiToIrOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            if (config.metadataKlib) {
                null
            } else {
                engine.runPsiToIr(frontendOutput, project, config.target)
            }
        }
        val headerKlibPath = config.headerKlibPath
        if (!headerKlibPath.isNullOrEmpty()) {
            val headerKlib = performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput, produceHeaderKlib = true, project)
            }
            performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                engine.writeKlib(headerKlib, headerKlibPath, produceHeaderKlib = true)
            }
            // Don't overwrite the header klib with the full klib and stop compilation here.
            // By providing the same path for both regular output and header klib we can skip emitting the full klib.
            if (File(config.outputPath).canonicalPath == File(headerKlibPath).canonicalPath) return null
        }
        return performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
            engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput, project = project)
        }
    }

    /**
     * Produce a single binary artifact.
     */
    private fun produceBinary(
            engine: PhaseEngine<PhaseContext>,
            config: BaseNativeConfig,
            environment: KotlinCoreEnvironment,
            project: Project,
    ) {
        val frontendOutput = engine.startFrontendEngine(config) { engine ->
            performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(environment, project) }
        } ?: return
        engine.startBackendEngine(config) { engine ->
            val psiToIrOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) { engine.runIrLinker(frontendOutput) }
            val backendContext = createBackendContext(engine.context.config, frontendOutput, psiToIrOutput)
            engine.runBackend(backendContext, psiToIrOutput.irModule, performanceManager)
        }
    }

    private fun produceBinaryFromBitcode(engine: PhaseEngine<PhaseContext>, config: BaseNativeConfig, bitcodeFilePath: String) {
        val llvmContext = LLVMContextCreate()!!
        var llvmModule: CPointer<LLVMOpaqueModule>? = null
        try {
            llvmModule = parseBitcodeFile(engine.context, engine.context.messageCollector, llvmContext, bitcodeFilePath)
            val depsPath = config.configuration.get(KonanConfigKeys.SERIALIZED_DEPENDENCIES)
            val dependencies = if (depsPath.isNullOrEmpty()) DependenciesTrackingResult(emptyList(), emptyList(), emptyList()).also {
                config.configuration.report(CompilerMessageSeverity.WARNING, "No backend dependencies provided.")
            } else DependenciesTrackingResult.deserialize(depsPath, File(depsPath).readStrings(), config)
            engine.startBackendEngine(config) { engine ->
                val context = BitcodePostProcessingContextImpl(engine.context.config, llvmModule, llvmContext)
                engine.runBitcodeBackend(context, dependencies)
            }
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
    private fun produceBundle(
            engine: PhaseEngine<PhaseContext>,
            config: BaseNativeConfig,
            environment: KotlinCoreEnvironment,
            project: Project,
    ) {
        require(config.target.family.isAppleFamily)

        val frontendOutput = engine.startFrontendEngine(config) { engine ->
            performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runK1Frontend(environment, project) }
        } ?: return

        engine.startBackendEngine(config) { engine ->
            val psiToIrOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
                engine.runPhase(CreateTestBundlePhase, frontendOutput)
                engine.runIrLinker(frontendOutput)
            }
            val backendContext = createBackendContext(engine.context.config, frontendOutput, psiToIrOutput)
            engine.runBackend(backendContext, psiToIrOutput.irModule, performanceManager)
        }
    }

    private fun createBackendContext(
            config: KonanConfig,
            frontendOutput: FrontendPhaseOutput.Full,
            psiToIrOutput: IrLinkerOutput,
            additionalDataSetter: (Context) -> Unit = {}
    ) = Context(
            config,
            frontendOutput.moduleDescriptor.getIncludedLibraryDescriptors(config).toSet() + frontendOutput.moduleDescriptor,
            frontendOutput.moduleDescriptor.builtIns as KonanBuiltIns,
            psiToIrOutput.irBuiltIns,
            psiToIrOutput.irModules,
            psiToIrOutput.irLinker,
            psiToIrOutput.symbols,
            psiToIrOutput.symbolTable,
    ).also {
        additionalDataSetter(it)
    }
}
