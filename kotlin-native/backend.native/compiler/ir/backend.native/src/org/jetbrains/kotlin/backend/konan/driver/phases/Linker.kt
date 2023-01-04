/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import java.io.File

internal data class LinkerPhaseInput(
        val outputs: LinkerOutputs,
        val objectFiles: List<File>,
        val dependenciesTrackingResult: DependenciesTrackingResult,
        val isCoverageEnabled: Boolean,
)

internal val LinkerPhase = createSimpleNamedCompilerPhase<PhaseContext, LinkerPhaseInput>(
        name = "Linker",
        description = "Linker"
) { context, input ->
    val linker = Linker(context)
    linker.link(input.outputs, input.objectFiles, input.dependenciesTrackingResult, isCoverageEnabled = input.isCoverageEnabled)
}

data class PreLinkPhaseInput(
        val objectFiles: List<File>,
        val outputObjectFile: File,
        val dependenciesTrackingResult: DependenciesTrackingResult,
)

internal val PreLinkCachesPhase = createSimpleNamedCompilerPhase<PhaseContext, PreLinkPhaseInput>(
        name = "PreLinkCaches",
        description = "create single big static file",
) { context, input ->
    val linker = Linker(context)
    linker.preLinkStaticCaches(input.objectFiles, input.outputObjectFile, input.dependenciesTrackingResult)
}