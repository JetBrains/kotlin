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
import org.jetbrains.kotlin.backend.konan.llvm.linkBitcodeDependenciesPhase
import org.jetbrains.kotlin.backend.konan.llvm.verifyBitcodePhase
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleFragmentImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

/**
 * Dynamic driver does not "know" upfront which phases will be executed.
 */
internal class DynamicCompilerDriver : CompilerDriver() {

    companion object {
        fun supportsConfig(config: KonanConfig): Boolean =
                config.produce == CompilerOutputKind.LIBRARY ||
                        config.produce.isCache
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                PhaseEngine.startTopLevel(config) { engine ->
                    when (config.produce) {
                        CompilerOutputKind.PROGRAM -> produceBinary(engine, config, environment)
                        CompilerOutputKind.DYNAMIC -> error("Dynamic compiler driver does not support `dynamic` output yet.")
                        CompilerOutputKind.STATIC -> error("Dynamic compiler driver does not support `static` output yet.")
                        CompilerOutputKind.FRAMEWORK -> error("Dynamic compiler driver does not support `framework` output yet.")
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

    private fun PhaseEngine<PhaseContext>.frontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
        val frontendOutput = useContext(FrontendContextImpl(config)) { it.runFrontend(environment) }
        return frontendOutput as? FrontendPhaseOutput.Full
    }

    private fun PhaseEngine<PhaseContext>.psiToIr(frontendOutput: FrontendPhaseOutput.Full, isProducingLibrary: Boolean): PsiToIrOutput {
        val config = this.context.config
        val psiToIrContext = PsiToIrContextImpl(config, frontendOutput.moduleDescriptor, frontendOutput.bindingContext)
        val psiToIrOutput = useContext(psiToIrContext) { psiToIrEngine ->
            val output = psiToIrEngine.runPsiToIr(frontendOutput, isProducingLibrary)
            psiToIrEngine.runSpecialBackendChecks(output)
            output
        }
        runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput.irModule)
        return psiToIrOutput
    }

    private fun createBackendContext(config: KonanConfig, frontendOutput: FrontendPhaseOutput.Full, psiToIrOutput: PsiToIrOutput): Context =
        Context(
            config,
            frontendOutput.environment,
            frontendOutput.frontendServices,
            frontendOutput.bindingContext,
            frontendOutput.moduleDescriptor
        ).also {
            it.populateAfterPsiToIr(psiToIrOutput)
        }

    private fun PhaseEngine<Context>.processModuleFragments(
        module: IrModuleFragment,
        action: (NativeGenerationState, IrModuleFragment) -> Unit
    ) {
        if (context.config.producePerFileCache) {
            val files = module.files.toList()
            files.asSequence().filterNot { it.isFunctionInterfaceFile }.forEach { file ->
                val moduleFragment = KonanIrModuleFragmentImpl(module.descriptor, module.irBuiltins, listOf(file))
                context.generationState =
                    NativeGenerationState(context, CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString()))
                if (context.generationState.shouldDefineFunctionClasses) {
                    moduleFragment.files += files.filter { it.isFunctionInterfaceFile }
                }
                action(context.generationState, moduleFragment)
            }
        } else {
            context.generationState = NativeGenerationState(context, context.config.libraryToCache?.strategy)
            action(context.generationState, module)
        }
    }

    private fun pickModuleToProcess(context: Context): IrModuleFragment = context.irModule!!

    private fun PhaseEngine<NativeGenerationState>.runAllLowerings(module: IrModuleFragment) {
        allLowerings.forEach { loweringPhase ->
            module.files.forEach { irFile ->
                this.runPhaseInParentContext(loweringPhase, irFile)
            }
        }
    }

    // TODO: This is a copy-paste from the old driver. Probably can be simpler in the new one.
    private fun PhaseEngine<NativeGenerationState>.runDependenciesLowering(module: IrModuleFragment) {
        val files = mutableListOf<IrFile>()
        files += module.files
        module.files.clear()

        val parentContext = context.parentContext

        // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
        parentContext.librariesWithDependencies
            .reversed()
            .forEach {
                val libModule = parentContext.irModules[it.libraryName]
                if (libModule == null || !context.llvmModuleSpecification.containsModule(libModule))
                    return@forEach

                module.files += libModule.files
                runAllLowerings(module)
                module.files.clear()
            }

        // Save all files for codegen in reverse topological order.
        // This guarantees that libraries initializers are emitted in correct order.
        parentContext.librariesWithDependencies
            .forEach {
                val libModule = parentContext.irModules[it.libraryName]
                if (libModule == null || !context.llvmModuleSpecification.containsModule(libModule))
                    return@forEach

                module.files += libModule.files
            }

        module.files += files
        println("How many files does it have after deps lower? ${module.files.size}")
    }

    private fun PhaseEngine<NativeGenerationState>.backendCodegen(module: IrModuleFragment, shouldLowerDependencies: Boolean) {
        runAllLowerings(module)
        if (shouldLowerDependencies) {
            runDependenciesLowering(module)
        }
        runPhaseInParentContext(bitcodePhase, module)
        runPhaseInParentContext(verifyBitcodePhase, module)
        runPhaseInParentContext(linkBitcodeDependenciesPhase, module)
        runPhaseInParentContext(bitcodePostprocessingPhase, module)
    }

    private fun PhaseEngine<NativeGenerationState>.lowerAndCompile(module: IrModuleFragment) {
        if (context.config.produce.isCache) {
            runPhase(BuildAdditionalCacheInfoPhase, module)
        }
        if (context.config.produce == CompilerOutputKind.PROGRAM) {
            runPhaseInParentContext(entryPointPhase, module)
        }
        backendCodegen(module, shouldLowerDependencies = !context.config.produce.isCache)
        if (context.config.produce.isCache) {
            runPhase(SaveAdditionalCacheInfoPhase)
        }

        // TODO: These phases don't actually need NativeGenerationState. We make them free later.
        run {
            // produceOutputPhase
            val output = context.tempFiles.nativeBinaryFileName
            context.bitcodeFileName = output
            // Insert `_main` after pipeline so we won't worry about optimizations
            // corrupting entry point.
            insertAliasToEntryPoint(context)
            LLVMWriteBitcodeToFile(context.llvm.module, output)
        }
        runPhaseInParentContext(objectFilesPhase)
        runPhaseInParentContext(linkerPhase)
        if (context.config.produce.isCache) {
            runPhase(FinalizeCachePhase)
        }
    }

    private fun produceBinary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.frontend(config, environment) ?: return
        val psiToIrOutput = engine.psiToIr(frontendOutput, isProducingLibrary = false)
        engine.useContext(createBackendContext(config, frontendOutput, psiToIrOutput)) { backendEngine ->
            backendEngine.runPhase(functionsWithoutBoundCheck)
            val module = pickModuleToProcess(backendEngine.context)
            backendEngine.processModuleFragments(module) { generationState, moduleFragment ->
                backendEngine.useContext(generationState) { generationStateEngine ->
                    generationStateEngine.lowerAndCompile(moduleFragment)
                }
            }
        }
    }


    private fun produceKlib(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.frontend(config, environment) ?: return
        val psiToIrOutput = if (config.metadataKlib) {
            null
        } else {
            engine.psiToIr(frontendOutput, isProducingLibrary = true)
        }
        val serializerOutput = engine.runSerializer(frontendOutput.moduleDescriptor, psiToIrOutput)
        engine.writeKlib(serializerOutput)
    }
}