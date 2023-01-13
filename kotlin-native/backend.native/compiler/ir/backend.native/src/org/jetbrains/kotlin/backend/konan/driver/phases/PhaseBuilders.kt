/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase

internal fun <Context : LoggingContext, Input, Output> createSimpleNamedCompilerPhase(
        name: String,
        description: String,
        outputIfNotEnabled: (PhaseConfigurationService, PhaserState<Input>, Context, Input) -> Output,
        phaseBody: (Context, Input) -> Output
): SimpleNamedCompilerPhase<Context, Input, Output> = object : SimpleNamedCompilerPhase<Context, Input, Output>(name, description) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
            outputIfNotEnabled(phaseConfig, phaserState, context, input)

    override fun phaseBody(context: Context, input: Input): Output =
            phaseBody(context, input)
}

internal fun <Context : LoggingContext, Input> createSimpleNamedCompilerPhase(
        name: String,
        description: String,
        phaseBody: (Context, Input) -> Unit
): SimpleNamedCompilerPhase<Context, Input, Unit> = object : SimpleNamedCompilerPhase<Context, Input, Unit>(name, description) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input) {}

    override fun phaseBody(context: Context, input: Input): Unit =
            phaseBody(context, input)
}

