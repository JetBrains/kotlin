/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.firFrontendWithLightTree
import org.jetbrains.kotlin.backend.konan.firFrontendWithPsi
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.pipeline.FirResult

sealed class FirOutput {
    object ShouldNotGenerateCode : FirOutput()

    data class Full(val firResult: FirResult) : FirOutput()
}

internal val FIRPhase = createSimpleNamedCompilerPhase(
        "FirFrontend",
        outputIfNotEnabled = { _, _, _, _ -> FirOutput.ShouldNotGenerateCode }
) { context: PhaseContext, input: KotlinCoreEnvironment ->
    if (input.configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)) {
        context.firFrontendWithLightTree(input)
    } else {
        context.firFrontendWithPsi(input)
    }
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFirFrontend(environment: KotlinCoreEnvironment): FirOutput {
    val languageVersion = environment.configuration.languageVersionSettings.languageVersion
    val kotlinSourceRoots = environment.configuration.kotlinSourceRoots
    if (!languageVersion.usesK2 && kotlinSourceRoots.isNotEmpty()) {
        throw Error("Attempt to run K2 from unsupported LV=${languageVersion}")
    }

    return this.runPhase(FIRPhase, environment)
}
