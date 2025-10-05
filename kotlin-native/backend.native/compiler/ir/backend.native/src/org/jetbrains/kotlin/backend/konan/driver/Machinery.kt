/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.AbstractKonanConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.ConfigChecks
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.util.PerformanceManager

internal open class BasicLightPhaseContext(
        override val config: AbstractKonanConfig,
) : LightPhaseContext {
    override var inVerbosePhase = false

    override val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    override fun dispose() {

    }

    override val performanceManager: PerformanceManager?
        get() = config.configuration.perfManager
}

internal interface PhaseContext : LightPhaseContext, ConfigChecks

internal open class BasicPhaseContext(
        override val config: KonanConfig,
) : BasicLightPhaseContext(config), PhaseContext

internal fun PhaseEngine.Companion.startTopLevel(config: KonanConfig, body: (PhaseEngine<PhaseContext>) -> Unit) {
    val phaserState = PhaserState()
    val phaseConfig = config.phaseConfig
    val context = BasicPhaseContext(config)
    val topLevelPhase = object : NamedCompilerPhase<PhaseContext, Any, Unit>("Compiler") {
        override fun phaseBody(context: PhaseContext, input: Any) {
            val engine = PhaseEngine(phaseConfig, phaserState, context)
            body(engine)
        }

        override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: PhaseContext, input: Any) {
            error("Compiler was disabled")
        }
    }
    topLevelPhase.invoke(phaseConfig, phaserState, context, Unit)
}
