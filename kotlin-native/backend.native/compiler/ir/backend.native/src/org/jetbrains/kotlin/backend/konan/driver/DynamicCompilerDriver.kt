/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendContext
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendPhase
import org.jetbrains.kotlin.backend.konan.driver.phases.PhaseEngine
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment

internal class DynamicCompilerDriver: CompilerDriver() {
    companion object {
        fun supportsConfig(): Boolean = false
    }

    override fun run(config: KonanConfig, environment: KotlinCoreEnvironment) {
        val engine = PhaseEngine(config.phaseConfig)

        val frontendContext = FrontendContext(config)
        val frontendResult = engine.runPhase(frontendContext, FrontendPhase, environment)
        if (frontendResult is FrontendPhaseResult.ShouldNotGenerateCode) {
            return
        }

    }
}