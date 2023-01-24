/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*

internal fun <Context : LoggingContext, Input, Output> createSimpleNamedCompilerPhase(
        name: String,
        description: String,
        preactions: Set<Action<Input, Context>> = emptySet(),
        postactions: Set<Action<Output, Context>> = emptySet(),
        prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
        outputIfNotEnabled: (PhaseConfigurationService, PhaserState<Input>, Context, Input) -> Output,
        op: (Context, Input) -> Output
): SimpleNamedCompilerPhase<Context, Input, Output> = object : SimpleNamedCompilerPhase<Context, Input, Output>(
        name,
        description,
        preactions = preactions,
        postactions = postactions.map { f ->
            fun(actionState: ActionState, data: Pair<Input, Output>, context: Context) = f(actionState, data.second, context)
        }.toSet(),
        prerequisite = prerequisite,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
            outputIfNotEnabled(phaseConfig, phaserState, context, input)

    override fun phaseBody(context: Context, input: Input): Output =
            op(context, input)
}

internal fun <Context : LoggingContext, Input> createSimpleNamedCompilerPhase(
        name: String,
        description: String,
        preactions: Set<Action<Input, Context>> = emptySet(),
        postactions: Set<Action<Input, Context>> = emptySet(),
        prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
        op: (Context, Input) -> Unit
): SimpleNamedCompilerPhase<Context, Input, Unit> = object : SimpleNamedCompilerPhase<Context, Input, Unit>(
        name,
        description,
        preactions = preactions,
        postactions = postactions.map { f ->
            fun(actionState: ActionState, data: Pair<Input, Unit>, context: Context) = f(actionState, data.first, context)
        }.toSet(),
        prerequisite = prerequisite,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input) {}

    override fun phaseBody(context: Context, input: Input): Unit =
            op(context, input)
}

