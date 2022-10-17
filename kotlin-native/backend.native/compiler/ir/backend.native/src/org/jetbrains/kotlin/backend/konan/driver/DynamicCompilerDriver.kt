/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
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
                        CompilerOutputKind.PROGRAM -> error("Dynamic compiler driver does not support `program` output yet.")
                        CompilerOutputKind.DYNAMIC -> error("Dynamic compiler driver does not support `dynamic` output yet.")
                        CompilerOutputKind.STATIC -> error("Dynamic compiler driver does not support `static` output yet.")
                        CompilerOutputKind.FRAMEWORK -> error("Dynamic compiler driver does not support `framework` output yet.")
                        CompilerOutputKind.LIBRARY -> produceKlib(engine, config, environment)
                        CompilerOutputKind.BITCODE -> error("Dynamic compiler driver does not support `bitcode` output yet.")
                        CompilerOutputKind.DYNAMIC_CACHE -> error("Dynamic compiler driver does not support `dynamic_cache` output yet.")
                        CompilerOutputKind.STATIC_CACHE -> error("Dynamic compiler driver does not support `static_cache` output yet.")
                        CompilerOutputKind.PRELIMINARY_CACHE -> TODO()
                    }
                }
            }
        }
    }

    private fun produceKlib(engine: PhaseEngine<PhaseContext>, config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendResult = engine.useContext(FrontendContextImpl(config)) { it.runFrontend(environment) }
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val psiToIrResult = if (!config.metadataKlib) {
            // TODO: Invalidate properly in PsiToIrContextImpl.dispose
            val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
            val psiToIrContext = PsiToIrContextImpl(config, frontendResult.moduleDescriptor, frontendResult.bindingContext, symbolTable)
            val psiToIrResult = engine.useContext(psiToIrContext) { psiToIrEngine ->
                val result = psiToIrEngine.runPsiToIr(frontendResult, isProducingLibrary = true)
                psiToIrEngine.runSpecialBackendChecks(result)
                result
            }
            engine.runPhase(engine.context, CopyDefaultValuesToActualPhase, psiToIrResult.irModule)
            psiToIrResult
        } else null
        val serializerResult = engine.runSerializer(frontendResult.moduleDescriptor, psiToIrResult)
        engine.writeKlib(serializerResult)
    }
}