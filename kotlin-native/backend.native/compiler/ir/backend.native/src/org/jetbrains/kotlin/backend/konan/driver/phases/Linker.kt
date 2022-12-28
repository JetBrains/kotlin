/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.konan.TempFiles

data class LinkerPhaseInput(
        val outputFile: String,
        val objectFiles: List<ObjectFile>,
        val dependenciesTrackingResult: DependenciesTrackingResult,
        val outputFiles: OutputFiles,
        val temporaryFiles: TempFiles,
        val isCoverageEnabled: Boolean,
)

internal val LinkerPhase = createSimpleNamedCompilerPhase<PhaseContext, LinkerPhaseInput>(
        name = "Linker",
        description = "Linker"
) { context, input ->
    val linker = Linker(context, input.isCoverageEnabled, input.temporaryFiles, input.outputFiles)
    linker.link(input.outputFile, input.objectFiles, input.dependenciesTrackingResult)
}