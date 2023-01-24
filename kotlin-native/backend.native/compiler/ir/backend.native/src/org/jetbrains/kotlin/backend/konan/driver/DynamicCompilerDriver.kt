/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.getIncludedLibraryDescriptors
import org.jetbrains.kotlin.backend.konan.isCache
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

/**
 * Dynamic driver does not "know" upfront which phases will be executed.
 */
internal class DynamicCompilerDriver : CompilerDriver() {

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                PhaseEngine.startTopLevel(config) { engine ->
                    when (config.produce) {
                        CompilerOutputKind.PROGRAM -> produceBinary(engine, config, environment)
                        CompilerOutputKind.DYNAMIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.STATIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.FRAMEWORK -> produceObjCFramework(engine, config, environment)
                        CompilerOutputKind.LIBRARY -> produceKlib(engine, config, environment)
                        CompilerOutputKind.BITCODE -> error("Bitcode output kind is obsolete.")
                        CompilerOutputKind.DYNAMIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.STATIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.PRELIMINARY_CACHE -> TODO()
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
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val objCExportedInterface = engine.runPhase(ProduceObjCExportInterfacePhase, frontendOutput)
        engine.runPhase(CreateObjCFrameworkPhase, CreateObjCFrameworkInput(frontendOutput.moduleDescriptor, objCExportedInterface))
        if (config.omitFrameworkBinary) {
            return
        }
        val (psiToIrOutput, objCCodeSpec) = engine.runPsiToIr(frontendOutput, isProducingLibrary = false) {
            it.runPhase(CreateObjCExportCodeSpecPhase, objCExportedInterface)
        }
        require(psiToIrOutput is PsiToIrOutput.ForBackend)
        val backendContext = createBackendContext(config, frontendOutput, psiToIrOutput) {
            it.objCExportedInterface = objCExportedInterface
            it.objCExportCodeSpec = objCCodeSpec
        }
        engine.runBackend(backendContext, psiToIrOutput.irModule)
    }

    private fun produceCLibrary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val (psiToIrOutput, cAdapterElements) = engine.runPsiToIr(frontendOutput, isProducingLibrary = false) {
            it.runPhase(BuildCExports, frontendOutput)
        }
        require(psiToIrOutput is PsiToIrOutput.ForBackend)
        val backendContext = createBackendContext(config, frontendOutput, psiToIrOutput) {
            it.cAdapterExportedElements = cAdapterElements
        }
        engine.runBackend(backendContext, psiToIrOutput.irModule)
    }

    private fun produceKlib(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val serializerOutput = if (environment.configuration.getBoolean(CommonConfigurationKeys.USE_FIR))
            serializeKLibK2(engine, environment)
        else
            serializeKlibK1(engine, config, environment)
        serializerOutput?.let { engine.writeKlib(it) }
    }

    private fun serializeKLibK2(
            engine: PhaseEngine<PhaseContext>,
            environment: KotlinCoreEnvironment
    ): SerializerOutput? {
        val frontendOutput = engine.runFirFrontend(environment)
        if (frontendOutput is FirOutput.ShouldNotGenerateCode) return null
        require(frontendOutput is FirOutput.Full)

        val fir2IrOutput = engine.runFir2Ir(frontendOutput)
//        engine.runK2SpecialBackendChecks(fir2IrOutput)  // TODO After fix of KT-56018 try uncommenting this line
        return engine.runFirSerializer(fir2IrOutput)
    }

    private fun serializeKlibK1(
            engine: PhaseEngine<PhaseContext>,
            config: KonanConfig,
            environment: KotlinCoreEnvironment
    ): SerializerOutput? {
        val frontendOutput = engine.runFrontend(config, environment) ?: return null
        val psiToIrOutput = if (config.metadataKlib) {
            null
        } else {
            engine.runPsiToIr(frontendOutput, isProducingLibrary = true) as PsiToIrOutput.ForKlib
        }
        return engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput)
    }

    /**
     * Produce a single binary artifact.
     */
    private fun produceBinary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val psiToIrOutput = engine.runPsiToIr(frontendOutput, isProducingLibrary = false)
        require(psiToIrOutput is PsiToIrOutput.ForBackend)
        val backendContext = createBackendContext(config, frontendOutput, psiToIrOutput)
        engine.runBackend(backendContext, psiToIrOutput.irModule)
    }

    private fun createBackendContext(
            config: KonanConfig,
            frontendOutput: FrontendPhaseOutput.Full,
            psiToIrOutput: PsiToIrOutput.ForBackend,
            additionalDataSetter: (Context) -> Unit = {}
    ) = Context(
            config,
            frontendOutput.moduleDescriptor.getIncludedLibraryDescriptors(config).toSet() + frontendOutput.moduleDescriptor,
            frontendOutput.moduleDescriptor.builtIns as KonanBuiltIns,
            psiToIrOutput.irModule.irBuiltins,
            psiToIrOutput.irModules,
            psiToIrOutput.irLinker,
            psiToIrOutput.symbols
    ).also {
        additionalDataSetter(it)
    }
}
