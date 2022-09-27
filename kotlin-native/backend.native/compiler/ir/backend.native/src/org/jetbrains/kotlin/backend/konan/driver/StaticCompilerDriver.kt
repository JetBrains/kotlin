/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendContext
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendPhase
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendPhaseResult
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal class StaticCompilerDriver: CompilerDriver() {
    companion object {
        fun supportsConfig(): Boolean =
                true
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        runTopLevelPhases(config, environment)
    }

    private fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

        val config = konanConfig.configuration

        val targets = konanConfig.targetManager
        if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
            targets.list()
        }

        val context = Context(konanConfig)
        context.environment = environment
        context.phaseConfig.konanPhasesConfig(konanConfig) // TODO: Wrong place to call it

        if (konanConfig.infoArgsOnly) return

        val frontendContext = FrontendContext(konanConfig)
        when (val frontendResult = FrontendPhase.phaseBody(frontendContext, environment)) {
            is FrontendPhaseResult.Full -> {
                context.moduleDescriptor = frontendResult.moduleDescriptor
                context.bindingContext = frontendResult.bindingContext
                context.frontendServices = frontendResult.frontendServices
            }
            FrontendPhaseResult.ShouldNotGenerateCode -> {
                return
            }
        }

        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                try {
                    toplevelPhase.cast<CompilerPhase<Context, Unit, Unit>>().invokeToplevel(context.phaseConfig, context, Unit)
                } finally {
                    context.disposeGenerationState()
                }
            }
        }
    }
}