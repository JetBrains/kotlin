/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.isCache
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder

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
                        CompilerOutputKind.FRAMEWORK -> if (!config.multipleFrameworks) {
                            produceObjCFramework(engine, config, environment)
                        } else {
                            produceMultipleObjCFrameworks(engine, config, environment)
                        }
                        CompilerOutputKind.LIBRARY -> produceKlib(engine, config, environment)
                        CompilerOutputKind.BITCODE -> error("Dynamic compiler driver does not support `bitcode` output yet.")
                        CompilerOutputKind.DYNAMIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.STATIC_CACHE -> produceBinary(engine, config, environment)
                        CompilerOutputKind.PRELIMINARY_CACHE -> TODO()
                    }
                }
            }
        }
    }

    // Experiment with producing framework per klib.
    private fun produceMultipleObjCFrameworks(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val modulesAndInterfaces = engine.runPhase(ProduceMultipleObjCExportInterfacesPhase, frontendOutput)
        modulesAndInterfaces.forEach { (translationConfig, iface) ->
            val outputPath = config.outputPath + "/${translationConfig.frameworkNaming.moduleName}"
            engine.runPhase(CreateObjCFrameworkPhase, CreateObjCFrameworkInput(translationConfig.module, iface, outputPath))
        }
        val (psiToIrOutput, objCCodeSpecs) = engine.runPsiToIr(frontendOutput, isProducingLibrary = false) { psiToIrEngine ->
            modulesAndInterfaces.values.map { iface -> iface to psiToIrEngine.runPhase(CreateObjCExportCodeSpecPhase, iface) }
        }
//        objCCodeSpecs.forEach { (iface, codespec) ->
//            println("CodeSpec for ${iface.namer.topLevelNamePrefix}")
//            printCodeSpec(codespec)
//        }
        // Walk from stdlib to exported libs
        val libraryWithObjcInfo = config.resolvedLibraries
                .filterRoots { (!it.isDefault && !config.purgeUserLibs) || it.isNeededForLink }
                .getFullList(TopologicalLibraryOrder)
                .map { it as KonanLibrary }
                .associateWith { library ->
                    objCCodeSpecs.first { (iface, _) -> iface.origins.first().kotlinLibrary == library }
                }
        libraryWithObjcInfo.forEach { (library, objcInfo) ->
            println("Codegen for ${library.libraryName}")
            val (iface, codespec) = objcInfo
            val backendContext = createBackendContext(config, iface.origins.first(), psiToIrOutput) {
                it.objCExportedInterface = iface
                it.objCExportCodeSpec = codespec
            }
            val irModule = psiToIrOutput.irModules[library.libraryName]
                    ?: psiToIrOutput.irModule
            // Compile each module to a separate binary
            engine.runBackend(backendContext, irModule, wholeWorld = false)
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
        engine.runPhase(CreateObjCFrameworkPhase, CreateObjCFrameworkInput(frontendOutput.moduleDescriptor, objCExportedInterface, config.outputPath))
        if (config.omitFrameworkBinary) {
            return
        }
        val (psiToIrOutput, objCCodeSpec) = engine.runPsiToIr(frontendOutput, isProducingLibrary = false) {
            it.runPhase(CreateObjCExportCodeSpecPhase, objCExportedInterface)
        }
        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, psiToIrOutput) {
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
        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, psiToIrOutput) {
            it.cAdapterExportedElements = cAdapterElements
        }
        engine.runBackend(backendContext, psiToIrOutput.irModule)
    }

    private fun produceKlib(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val psiToIrOutput = if (config.metadataKlib) {
            null
        } else {
            engine.runPsiToIr(frontendOutput, isProducingLibrary = true)
        }
        val serializerOutput = engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput)
        engine.writeKlib(serializerOutput)
    }

    /**
     * Produce a single binary artifact.
     */
    private fun produceBinary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val psiToIrOutput = engine.runPsiToIr(frontendOutput, isProducingLibrary = false)
        val backendContext = createBackendContext(config, frontendOutput.moduleDescriptor, psiToIrOutput)
        val module = if (config.produce.isCache) {
            psiToIrOutput.irModules[config.libraryToCache!!.klib.libraryName]
                    ?: error("No module for the library being cached: ${config.libraryToCache!!.klib.libraryName}")
        } else {
            psiToIrOutput.irModule
        }
        engine.runBackend(backendContext, module, wholeWorld = !config.produce.isCache)
    }

    private fun createBackendContext(
            config: KonanConfig,
            moduleDescriptor: ModuleDescriptor,
            psiToIrOutput: PsiToIrOutput,
            additionalDataSetter: (Context) -> Unit = {}
    ) = Context(
            config,
            psiToIrOutput.irModule.irBuiltins,
            moduleDescriptor,
            psiToIrOutput.irModules,
    ).also {
        it.populateAfterPsiToIr(psiToIrOutput)
        additionalDataSetter(it)
    }
}