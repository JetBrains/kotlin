/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.driver.phases.PhaseEngine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

internal class DynamicCompilerDriver: CompilerDriver() {
    companion object {
        fun supportsConfig(config: KonanConfig): Boolean {
            if (config.produce == CompilerOutputKind.LIBRARY) {
                return true
            }
            return false
        }
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                when (config.produce) {
                    CompilerOutputKind.PROGRAM -> TODO()
                    CompilerOutputKind.DYNAMIC -> TODO()
                    CompilerOutputKind.STATIC -> TODO()
                    CompilerOutputKind.FRAMEWORK -> TODO()
                    CompilerOutputKind.LIBRARY -> produceKlib(config, environment)
                    CompilerOutputKind.BITCODE -> TODO()
                    CompilerOutputKind.DYNAMIC_CACHE -> TODO()
                    CompilerOutputKind.STATIC_CACHE -> TODO()
                    CompilerOutputKind.PRELIMINARY_CACHE -> TODO()
                }
            }
        }
    }

    private fun produceKlib(config: KonanConfig, environment: KotlinCoreEnvironment) {
        PhaseEngine.startTopLevel(config) { engine ->
            val frontendResult = engine.runFrontend(config, environment)
            if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
                return@startTopLevel
            }
            require(frontendResult is FrontendPhaseResult.Full)
            val psiToIrResult = SymbolTableResource().use { symbolTable ->
                engine.runPsiToIr(config, frontendResult, symbolTable, isProducingLibrary = true)
            }
            require(psiToIrResult is PsiToIrResult.Full)
            val serializerResult = engine.runSerializer(config, frontendResult.moduleDescriptor, psiToIrResult)
            engine.writeKlib(config, serializerResult)
        }
    }
}