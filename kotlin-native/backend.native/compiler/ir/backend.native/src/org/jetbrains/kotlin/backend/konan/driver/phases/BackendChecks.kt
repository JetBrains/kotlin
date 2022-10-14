/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal

internal val SpecialBackendChecksPhase = object : SimpleNamedCompilerPhase<PsiToIrContext, PsiToIrResult, Unit>(
        "SpecialBackendChecks",
        "Special backend checks"
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<PsiToIrResult>, context: PsiToIrContext, input: PsiToIrResult) {

    }

    override fun phaseBody(context: PsiToIrContext, input: PsiToIrResult) {
        SpecialBackendChecksTraversal(context, context.interopBuiltIns, input.symbols, input.irModule.irBuiltins).lower(input.irModule)
    }
}

internal fun <T : PsiToIrContext> PhaseEngine<T>.runSpecialBackendChecks(psiToIrResult: PsiToIrResult) {
    runPhase(context, SpecialBackendChecksPhase, psiToIrResult)
}