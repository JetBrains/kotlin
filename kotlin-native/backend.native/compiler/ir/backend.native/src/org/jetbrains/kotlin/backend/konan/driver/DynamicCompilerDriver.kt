/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.driver.phases.PhaseEngine
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

internal class DynamicCompilerDriver: CompilerDriver() {
    companion object {
        fun supportsConfig(): Boolean {
            return false
        }
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        PhaseEngine.startTopLevel(config) { engine ->
            usingNativeMemoryAllocator {
                usingJvmCInteropCallbacks {
                    when (config.produce) {
                        CompilerOutputKind.PROGRAM -> produceProgram(engine, config, environment)
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

    private fun produceKlib(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendResult = engine.useContext(FrontendContextImpl(config)) { frontendEngine ->
            frontendEngine.runFrontend(environment)
        }
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val psiToIrResult = if (!config.metadataKlib) {
            val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
            val psiToIrContext = PsiToContextImpl(config, frontendResult.moduleDescriptor, symbolTable)
            engine.useContext(psiToIrContext) { psiToIrEngine ->
                psiToIrEngine.runPsiToIr(frontendResult, isProducingLibrary = true)
            }
        } else null
        engine.useContext(BasicPhaseContext(config)) { serializationEngine ->
            val serializerResult = serializationEngine.runSerializer(frontendResult.moduleDescriptor, psiToIrResult)
            serializationEngine.writeKlib(serializerResult)
        }

    }

    private fun produceFramework(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendResult = engine.useContext(FrontendContextImpl(config)) { frontendEngine ->
            frontendEngine.runFrontend(environment)
        }
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val objcInterface = engine.produceObjCExportInterface(frontendResult)
        val frameworkFile = run {
            val outputPath = config.cacheSupport.tryGetImplicitOutput() ?: config.outputPath
            val outputFiles = OutputFiles(outputPath, config.target, config.produce)
            outputFiles.mainFile
        }
        engine.writeObjCFramework(objcInterface, frontendResult.moduleDescriptor, frameworkFile)
        if (config.omitFrameworkBinary) {
            return
        }
        val (psiToIrResult, objCCodeSpec) = run {
            val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
            val psiToIrContext = PsiToContextImpl(config, frontendResult.moduleDescriptor, symbolTable)
            engine.useContext(psiToIrContext) { psiToIrEngine ->
                val objCCodeSpec = psiToIrEngine.produceObjCCodeSpec(objcInterface)
                val psiToIrResult = psiToIrEngine.runPsiToIr(frontendResult, isProducingLibrary = false)
                Pair(psiToIrResult, objCCodeSpec)
            }
        }
        // Let's "eat" Context step-by-step. We don't use it for frontend and psi2ir,
        // but use it for lowerings and bitcode generation for now.
        val context = Context(config).also {
            it.populateAfterFrontend(frontendResult)
            it.populateAfterPsiToIr(psiToIrResult)
            it.objCExport = ObjCExport(it, objcInterface, objCCodeSpec)
        }
        engine.useContext(context) { middleEndEngine ->
            middleEndEngine.runPhase(context, functionsWithoutBoundCheck, Unit)
            middleEndEngine.useContext(NativeGenerationState(context)) { nativeGenerationEngine ->
                // TODO: Drop this property, use generation state separately.
                context.generationState = nativeGenerationEngine.context
                middleEndEngine.runBackendCodegen(context.irModule!!)
                val bitcodeFile = nativeGenerationEngine.writeBitcodeFile(nativeGenerationEngine.context.llvm.module)
                // TODO: These two phases should not use NativeGenerationEngine. Instead, they should use their own.
                //  Probably separate, because in the future we want linker to accumulate results.
                val objectFiles = nativeGenerationEngine.produceObjectFiles(bitcodeFile)
                nativeGenerationEngine.linkObjectFiles(objectFiles, context.llvmModuleSpecification, context.coverage.enabled)
            }
        }
    }

    private fun produceProgram(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendResult = engine.useContext(FrontendContextImpl(config)) { frontendEngine ->
            frontendEngine.runFrontend(environment)
        }
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val psiToIrResult = run {
            val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
            val psiToIrContext = PsiToContextImpl(config, frontendResult.moduleDescriptor, symbolTable)
            engine.useContext(psiToIrContext) { psiToIrEngine ->
                psiToIrEngine.runPsiToIr(frontendResult, isProducingLibrary = false)
            }
        }
        // Let's "eat" Context step-by-step. We don't use it for frontend and psi2ir,
        // but use it for lowerings and bitcode generation for now.
        val context = Context(config).also {
            it.populateAfterFrontend(frontendResult)
            it.populateAfterPsiToIr(psiToIrResult)
            it.objCExport = ObjCExport(it, null, null)
        }
        engine.useContext(context) { middleEndEngine ->
            middleEndEngine.runPhase(context, functionsWithoutBoundCheck, Unit)
            middleEndEngine.useContext(NativeGenerationState(context)) { nativeGenerationEngine ->
                // TODO: Drop this property, use generation state separately.
                context.generationState = nativeGenerationEngine.context
                middleEndEngine.runBackendCodegen(context.irModule!!)
                val module = context.generationState.llvm.module
                insertAliasToEntryPoint(context, module)
                val bitcodeFile = nativeGenerationEngine.writeBitcodeFile(module)
                // TODO: These two phases should not use NativeGenerationEngine. Instead, they should use their own.
                //  Probably separate, because in the future we want linker to accumulate results.
                val objectFiles = nativeGenerationEngine.produceObjectFiles(bitcodeFile)
                nativeGenerationEngine.linkObjectFiles(objectFiles, context.llvmModuleSpecification, context.coverage.enabled)
            }
        }
    }
}