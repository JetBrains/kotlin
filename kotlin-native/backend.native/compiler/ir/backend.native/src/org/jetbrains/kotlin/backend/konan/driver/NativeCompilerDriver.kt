/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

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
import org.jetbrains.kotlin.config.nativeBinaryOptions.CInterfaceGenerationMode
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.usingNativeMemoryAllocator

/**
 * Driver orchestrates and connects different parts of the compiler into a complete pipeline.
 */
internal class NativeCompilerDriver(private val performanceManager: PerformanceManager?) {

    fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                PhaseEngine.startTopLevel(config) { engine ->
                    if (!config.compileFromBitcode.isNullOrEmpty()) produceBinaryFromBitcode(engine, config, config.compileFromBitcode!!)
                    else when (config.produce) {
                        CompilerOutputKind.PROGRAM -> produceBinary(engine, config, environment)
                        CompilerOutputKind.DYNAMIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.STATIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.FRAMEWORK -> produceObjCFramework(engine, config, environment)
                        CompilerOutputKind.LIBRARY -> produceKlib(engine, config, environment)
                        CompilerOutputKind.BITCODE -> error("Bitcode output kind is obsolete.")
                        CompilerOutputKind.DYNAMIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.STATIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.HEADER_CACHE -> produceBinary(engine, config, environment)
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
     */
    private fun produceObjCFramework(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFrontend(config, environment) } ?: return

        val (objCExportedInterface, linkKlibsOutput, objCCodeSpec) = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            val objCExportedInterface = engine.runPhase(ProduceObjCExportInterfacePhase, frontendOutput)
            engine.runPhase(CreateObjCFrameworkPhase, CreateObjCFrameworkInput(frontendOutput.moduleDescriptor, objCExportedInterface))
            val (linkKlibsOutput, objCCodeSpec) = engine.linkKlibs(frontendOutput) {
                it.runPhase(CreateObjCExportCodeSpecPhase, objCExportedInterface)
            }
            if (config.omitFrameworkBinary) {
                return
            }
            Triple(objCExportedInterface, linkKlibsOutput, objCCodeSpec)
        }

        val backendContext = createBackendContext(config, frontendOutput, linkKlibsOutput) {
            it.objCExportedInterface = objCExportedInterface
            it.objCExportCodeSpec = objCCodeSpec
        }
        engine.runBackend(backendContext, linkKlibsOutput.irModule, performanceManager)
    }

    private fun produceCLibrary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFrontend(config, environment) } ?: return

        val (linkKlibsOutput, cAdapterElements) = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            engine.linkKlibs(frontendOutput) {
                if (config.cInterfaceGenerationMode == CInterfaceGenerationMode.V1) {
                    it.runPhase(BuildCExports, frontendOutput)
                } else {
                    null
                }
            }
        }
        val backendContext = createBackendContext(config, frontendOutput, linkKlibsOutput) {
            it.cAdapterExportedElements = cAdapterElements
        }
        engine.runBackend(backendContext, linkKlibsOutput.irModule, performanceManager)
    }

    private fun produceKlib(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val serializerOutput = if (environment.configuration.getBoolean(CommonConfigurationKeys.USE_FIR))
            serializeKLibK2(engine, config, environment)
        else
            serializeKlibK1(engine, config, environment)
        serializerOutput?.let {
            performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                engine.writeKlib(it)
            }
        }
    }

    private fun serializeKLibK2(
            engine: PhaseEngine<PhaseContext>,
            config: KonanConfig,
            environment: KotlinCoreEnvironment
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
                engine.runFir2Ir(frontendOutput)
            }
            engine.runK2SpecialBackendChecks(fir2IrOutput)

            val loweredIr = performanceManager.tryMeasurePhaseTime(PhaseType.IrPreLowering) {
                engine.runPreSerializationLowerings(fir2IrOutput, environment)
            }
            val headerKlibPath = config.headerKlibPath
            if (!headerKlibPath.isNullOrEmpty()) {
                // Child performance manager is needed since otherwise the phase ordering is broken
                PerformanceManagerImpl.createAndEnableChildIfNeeded(performanceManager).let {
                    it?.notifyPhaseFinished(PhaseType.Initialization)

                    val headerKlib = it.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                        engine.runFir2IrSerializer(FirSerializerInput(loweredIr, produceHeaderKlib = true))
                    }
                    it.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                        engine.writeKlib(headerKlib, headerKlibPath, produceHeaderKlib = true)
                    }
                    performanceManager?.addOtherUnitStats(it?.unitStats)
                }
                // Don't overwrite the header klib with the full klib and stop compilation here.
                // By providing the same path for both regular output and header klib we can skip emitting the full klib.
                if (File(config.outputPath).canonicalPath == File(headerKlibPath).canonicalPath) return null
            }

            performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                engine.runFir2IrSerializer(FirSerializerInput(loweredIr))
            }
        }
    }

    private fun serializeKlibK1(
            engine: PhaseEngine<PhaseContext>,
            config: KonanConfig,
            environment: KotlinCoreEnvironment
    ): SerializerOutput? {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFrontend(config, environment) } ?: return null
        val psiToIrOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            if (config.metadataKlib) {
                null
            } else {
                engine.runPsiToIr(frontendOutput)
            }
        }
        val headerKlibPath = config.headerKlibPath
        if (!headerKlibPath.isNullOrEmpty()) {
            // Child performance manager is needed since otherwise the phase ordering is broken
            PerformanceManagerImpl.createAndEnableChildIfNeeded(performanceManager).let {
                it?.notifyPhaseFinished(PhaseType.Initialization)

                val headerKlib = it.tryMeasurePhaseTime(PhaseType.IrSerialization) {
                    engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput, produceHeaderKlib = true)
                }
                it.tryMeasurePhaseTime(PhaseType.KlibWriting) {
                    engine.writeKlib(headerKlib, headerKlibPath, produceHeaderKlib = true)
                }
                performanceManager?.addOtherUnitStats(it?.unitStats)
            }
            // Don't overwrite the header klib with the full klib and stop compilation here.
            // By providing the same path for both regular output and header klib we can skip emitting the full klib.
            if (File(config.outputPath).canonicalPath == File(headerKlibPath).canonicalPath) return null
        }
        return performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
            engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput)
        }
    }

    /**
     * Produce a single binary artifact.
     */
    private fun produceBinary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFrontend(config, environment) } ?: return

        val linkKlibsOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) { engine.linkKlibs(frontendOutput) }
        val backendContext = createBackendContext(config, frontendOutput, linkKlibsOutput)
        engine.runBackend(backendContext, linkKlibsOutput.irModule, performanceManager)
    }

    private fun produceBinaryFromBitcode(engine: PhaseEngine<PhaseContext>, config: KonanConfig, bitcodeFilePath: String) {
        val llvmContext = LLVMContextCreate()!!
        var llvmModule: CPointer<LLVMOpaqueModule>? = null
        try {
            llvmModule = parseBitcodeFile(engine.context, engine.context.messageCollector, llvmContext, bitcodeFilePath)
            val context = BitcodePostProcessingContextImpl(config, llvmModule, llvmContext)
            val depsPath = config.readSerializedDependencies
            val dependencies = if (depsPath.isNullOrEmpty()) DependenciesTrackingResult(emptyList(), emptyList(), emptyList()).also {
                config.configuration.report(CompilerMessageSeverity.WARNING, "No backend dependencies provided.")
            } else DependenciesTrackingResult.deserialize(depsPath, File(depsPath).readStrings(), config)
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
    private fun produceBundle(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        require(config.target.family.isAppleFamily)
        require(config.produce == CompilerOutputKind.TEST_BUNDLE)

        val frontendOutput = performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) { engine.runFrontend(config, environment) } ?: return
        val linkKlibsOutput = performanceManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            engine.runPhase(CreateTestBundlePhase, frontendOutput)
            engine.linkKlibs(frontendOutput)
        }
        val backendContext = createBackendContext(config, frontendOutput, linkKlibsOutput)
        engine.runBackend(backendContext, linkKlibsOutput.irModule, performanceManager)
    }

    private fun createBackendContext(
            config: KonanConfig,
            frontendOutput: FrontendPhaseOutput.Full,
            linkKlibsOutput: LinkKlibsOutput,
            additionalDataSetter: (Context) -> Unit = {}
    ) = Context(
            config,
            frontendOutput.moduleDescriptor.getIncludedLibraryDescriptors(config).toSet() + frontendOutput.moduleDescriptor,
            frontendOutput.moduleDescriptor.builtIns as KonanBuiltIns,
            linkKlibsOutput.irBuiltIns,
            linkKlibsOutput.irModules,
            linkKlibsOutput.irLinker,
            linkKlibsOutput.symbols,
            linkKlibsOutput.symbolTable,
    ).also {
        additionalDataSetter(it)
    }
}
