/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.driver.phases.PhaseEngine
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

internal class DynamicCompilerDriver: CompilerDriver() {
    companion object {
        fun supportsConfig(config: KonanConfig): Boolean {
            if (config.produce == CompilerOutputKind.LIBRARY) {
                return true
            }
            if (config.produce == CompilerOutputKind.FRAMEWORK) {
                return true
            }
            return false
        }
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        PhaseEngine.startTopLevel(config) { engine ->
            usingNativeMemoryAllocator {
                usingJvmCInteropCallbacks {
                    when (config.produce) {
                        CompilerOutputKind.PROGRAM -> TODO()
                        CompilerOutputKind.DYNAMIC -> TODO()
                        CompilerOutputKind.STATIC -> TODO()
                        CompilerOutputKind.FRAMEWORK -> produceFramework(engine, config, environment)
                        CompilerOutputKind.LIBRARY -> produceKlib(engine, config, environment)
                        CompilerOutputKind.BITCODE -> TODO()
                        CompilerOutputKind.DYNAMIC_CACHE -> TODO()
                        CompilerOutputKind.STATIC_CACHE -> TODO()
                        CompilerOutputKind.PRELIMINARY_CACHE -> TODO()
                    }
                }
            }
        }
    }

    private fun produceKlib(engine: PhaseEngine, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendResult = engine.runFrontend(config, environment)
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val psiToIrResult = SymbolTableResource().use { symbolTable ->
            engine.runPsiToIr(config, frontendResult, symbolTable, isProducingLibrary = true)
        }
        require(psiToIrResult is PsiToIrResult.Full)
        val serializerResult = engine.runSerializer(config, frontendResult.moduleDescriptor, psiToIrResult)
        engine.writeKlib(config, serializerResult)
    }

    private fun produceFramework(engine: PhaseEngine, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendResult = engine.runFrontend(config, environment)
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val objcInterface = engine.produceObjCExportInterface(config, frontendResult)

        if (config.omitFrameworkBinary) {
            val outputPath = config.cacheSupport.tryGetImplicitOutput() ?: config.outputPath
            val outputFiles = OutputFiles(outputPath, config.target, config.produce)
            val frameworkFile = outputFiles.mainFile
            engine.writeObjCFramework(config, objcInterface, frontendResult.moduleDescriptor, frameworkFile)
            return
        }
        val (psiToIrResult, objCCodeSpec) = SymbolTableResource().use { symbolTable ->
            val objCCodeSpec = engine.produceObjCCodeSpec(config, objcInterface, symbolTable)
            val psiToIrResult = engine.runPsiToIr(config, frontendResult, symbolTable, isProducingLibrary = true)
            Pair(psiToIrResult, objCCodeSpec)
        }
        require(psiToIrResult is PsiToIrResult.Full)
        val context = Context(config)
        context.populateAfterPsiToIr(psiToIrResult)
        context.objCExport = ObjCExport(context, objcInterface, objCCodeSpec)
        val nativeGenerationState = NativeGenerationState(context)
        context.generationState = nativeGenerationState

        // Let's "eat" Context step-by-step. We don't use it for frontend, psi2ir, and object files,
        // but use it for lowerings and bitcode generation.
        engine.runBackendCodegen(context, context.irModule!!)

        val objectFiles = engine.produceObjectFiles(
                config,
                context.bitcodeFileName,
                context.generationState.tempFiles
        )
        engine.linkObjectFiles(
                config,
                objectFiles,
                context.generationState.llvm,
                context.llvmModuleSpecification,
                context.coverage.enabled,
                context.generationState.outputFile,
                context.generationState.outputFiles,
                context.generationState.tempFiles,
        )
    }
}