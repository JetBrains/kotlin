/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator
import org.jetbrains.kotlin.konan.file.File

/**
 * Dynamic driver does not "know" upfront which phases will be executed.
 */
internal class DynamicCompilerDriver : CompilerDriver() {

    companion object {
        fun supportsConfig(config: KonanConfig): Boolean =
                config.produce != CompilerOutputKind.BITCODE
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                PhaseEngine.startTopLevel(config) { engine ->
                    when (config.produce) {
                        CompilerOutputKind.PROGRAM -> produceBinary(engine, config, environment)
                        CompilerOutputKind.DYNAMIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.STATIC -> produceCLibrary(engine, config, environment)
                        CompilerOutputKind.FRAMEWORK -> produceFramework(engine, config, environment)
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

    private fun produceCLibrary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val (psiToIrOutput, cexportResult) = engine.runPsiToIr(frontendOutput, isProducingLibrary = false) { psiToIrEngine ->
            psiToIrEngine.runPhase(BuildCExports, frontendOutput)
        }
        val backendContext = createBackendContext(config, frontendOutput, psiToIrOutput) {
            it.cexportResults = cexportResult
        }
        engine.runBackend(backendContext)
    }

    private fun produceFramework(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val objCExportedInterface = engine.runPhase(ProduceObjCExportInterfacePhase, frontendOutput)
        if (config.omitFrameworkBinary) {
            val outputFiles = OutputFiles(config.outputPath, config.target, config.produce)
            ObjCExport(engine.context, frontendOutput.moduleDescriptor, objCExportedInterface, null)
                    .produceFrameworkInterface(File(outputFiles.mainFileName))
            return
        }
        val (psiToIrOutput, objCCodeSpec) = engine.runPsiToIr(frontendOutput, isProducingLibrary = false) {
            it.runPhase(CreateObjCExportCodeSpecPhase, objCExportedInterface)
        }
        val backendContext = createBackendContext(config, frontendOutput, psiToIrOutput) {
            it.objCExportedInterface = objCExportedInterface
            it.objCExportCodeSpec = objCCodeSpec
        }
        engine.runBackend(backendContext)
    }

    /**
     * A common default pipeline to produce a binary output.
     */
    private fun produceBinary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val psiToIrOutput = engine.runPsiToIr(frontendOutput, isProducingLibrary = false)
        val backendContext = createBackendContext(config, frontendOutput, psiToIrOutput)
        engine.runBackend(backendContext)
    }

    private fun <C : PhaseContext> PhaseEngine<C>.runBackend(backendContext: Context) {
        useContext(backendContext) { backendEngine ->
            backendEngine.runPhase(functionsWithoutBoundCheck)
            backendEngine.processModuleFragments(backendEngine.context.irModule!!) { generationState, fragment ->
                backendEngine.useContext(generationState) { generationStateEngine ->
                    // TODO: We can run compile part in parallel if we get rid of context.generationState.
                    generationStateEngine.runLowerAndCompile(fragment)
                }
            }
        }
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
}