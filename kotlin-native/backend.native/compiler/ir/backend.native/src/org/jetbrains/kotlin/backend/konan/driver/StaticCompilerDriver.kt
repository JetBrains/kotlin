/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendContextImpl
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendPhase
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendPhaseOutput
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

/**
 * Static compiler uses statically-defined compilation pipeline and a single Context during whole compilation.
 * Superseded by [DynamicCompilerDriver] and will be removed once dynamic driver become complete.
 */
internal class StaticCompilerDriver : CompilerDriver() {
    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        runTopLevelPhases(config, environment)
    }

    private fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

        val config = konanConfig.configuration

        val targets = konanConfig.targetManager
        if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
            targets.list()
        }

        if (konanConfig.infoArgsOnly) return

        val frontendContext = FrontendContextImpl(konanConfig)
        val frontendOutput = FrontendPhase.phaseBody(frontendContext, environment)
        if (frontendOutput is FrontendPhaseOutput.ShouldNotGenerateCode) {
            return
        }
        require(frontendOutput is FrontendPhaseOutput.Full)
        val context = Context(
                konanConfig,
                environment,
                frontendOutput.frontendServices,
                frontendOutput.bindingContext,
                frontendOutput.moduleDescriptor,
        )
        context.phaseConfig.konanPhasesConfig(konanConfig) // TODO: Wrong place to call it

        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                try {
                    toplevelPhase.invokeToplevel(context.phaseConfig, context, Unit)
                } finally {
                    context.disposeGenerationState()
                }
            }
        }
    }
}