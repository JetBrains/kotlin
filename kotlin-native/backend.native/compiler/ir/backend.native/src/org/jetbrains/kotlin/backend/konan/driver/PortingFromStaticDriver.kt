/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.common.phaser.AbstractNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.changePhaserStateType
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState

/**
 * This is a hack.
 *
 * It simplifies porting phases that are logically running in [NativeGenerationState] context, but
 * for legacy reasons require [Context].
 *
 * The idea is following:
 * 1. Port everything from the static driver.
 * 2. Adapt phases to use NativeGenerationState and pass additional input directly instead of taking it from Context.
 * 3. Drop this hack.
 *
 * Idea of "parent context" might be appealing at the first sight. Unfortunately, it has serious drawbacks:
 * 1. It couples child context with the parent one which makes it harder to reuse child context in different environment (e.g. tests).
 * 2. It couples phases that are running in child context with the parent one.
 */
internal fun <Input, Output> PhaseEngine<NativeGenerationState>.runPhaseInParentContext(
        phase: AbstractNamedCompilerPhase<Context, Input, Output>,
        input: Input
): Output {
    return phase.invoke(phaseConfig, phaserState.changePhaserStateType(), context.context, input)
}