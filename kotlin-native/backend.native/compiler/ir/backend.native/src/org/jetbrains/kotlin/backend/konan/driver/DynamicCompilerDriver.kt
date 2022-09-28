/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.LlvmModuleSpecification
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendContext
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendPhase
import org.jetbrains.kotlin.backend.konan.driver.phases.PhaseEngine
import org.jetbrains.kotlin.backend.konan.toplevelPhase
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal class DynamicCompilerDriver: CompilerDriver() {
    companion object {
        fun supportsConfig(): Boolean = false
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
        val engine = PhaseEngine(config.phaseConfig)

        val frontendResult = engine.runFrontend(config, environment)
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }
        require(frontendResult is FrontendPhaseResult.Full)
        val psiToIrResult = SymbolTableResource().use { symbolTable ->
            engine.runPsiToIr(config, frontendResult, symbolTable, isProducingLibrary = true)
        }
        require(psiToIrResult is PsiToIrResult.ForLibrary)
        val serializerResult = engine.runSerializer(config, frontendResult.moduleDescriptor, psiToIrResult)
        engine.writeKlib(config, serializerResult)
    }
}