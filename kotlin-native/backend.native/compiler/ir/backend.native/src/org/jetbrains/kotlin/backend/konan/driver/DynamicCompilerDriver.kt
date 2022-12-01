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
import org.jetbrains.kotlin.backend.konan.shouldDefineFunctionClasses
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

/**
 * Dynamic driver does not "know" upfront which phases will be executed.
 */
internal class DynamicCompilerDriver : CompilerDriver() {

    companion object {
        fun supportsConfig(config: KonanConfig): Boolean =
                config.produce == CompilerOutputKind.LIBRARY
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

    /**
     * A common default pipeline to produce a binary output.
     */
    private fun produceBinary(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendOutput = engine.runFrontend(config, environment) ?: return
        val psiToIrOutput = engine.runPsiToIr(frontendOutput, isProducingLibrary = false)
        engine.useContext(createBackendContext(config, frontendOutput, psiToIrOutput)) { backendEngine ->
            backendEngine.runPhase(functionsWithoutBoundCheck)
            backendEngine.processModuleFragments(backendEngine.context.irModule!!).forEach { (generationState, fragment) ->
                backendEngine.context.generationState = generationState
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