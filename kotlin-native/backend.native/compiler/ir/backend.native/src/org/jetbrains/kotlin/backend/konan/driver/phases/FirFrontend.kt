/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.firFrontend
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.pipeline.FirResult

sealed class FirOutput {
    object ShouldNotGenerateCode : FirOutput()

    data class Full(val firResult: FirResult) : FirOutput()
}

internal val FIRPhase = createSimpleNamedCompilerPhase(
        "FirFrontend", "Compiler Fir Frontend",
        outputIfNotEnabled = { _, _, _, _ -> FirOutput.ShouldNotGenerateCode }
) { context: PhaseContext, input: KotlinCoreEnvironment ->
    context.firFrontend(input)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFirFrontend(environment: KotlinCoreEnvironment): FirOutput {
    return this.runPhase(FIRPhase, environment)
}
