/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.ConfigChecks
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * A version of [NativePhaseContext] that is specific to the Native backend.
 */
internal interface NativeBackendPhaseContext : NativePhaseContext, ConfigChecks

internal open class BasicNativeBackendPhaseContext(
        override val config: NativeSecondStageCompilationConfig,
) : NativeBackendPhaseContext {
    override var inVerbosePhase = false

    override val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    override fun dispose() {

    }

    override val performanceManager: PerformanceManager?
        get() = config.configuration.perfManager
}

internal fun PhaseEngine.Companion.startTopLevel(config: NativeSecondStageCompilationConfig, body: (PhaseEngine<NativeBackendPhaseContext>) -> Unit) {
    val phaserState = PhaserState()
    val phaseConfig = config.phaseConfig
    val context = BasicNativeBackendPhaseContext(config)
    val topLevelPhase = object : NamedCompilerPhase<NativeBackendPhaseContext, Any, Unit>("Compiler") {
        override fun phaseBody(context: NativeBackendPhaseContext, input: Any) {
            val engine = PhaseEngine(phaseConfig, phaserState, context)
            body(engine)
        }

        override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: NativeBackendPhaseContext, input: Any) {
            error("Compiler was disabled")
        }
    }
    topLevelPhase.invoke(phaseConfig, phaserState, context, Unit)
}
