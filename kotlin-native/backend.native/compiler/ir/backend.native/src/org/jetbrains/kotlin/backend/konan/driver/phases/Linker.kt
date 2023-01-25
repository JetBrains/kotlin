/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import java.io.File

internal data class LinkerPhaseInput(
        val outputFile: String,
        val outputKind: LinkerOutputKind,
        val objectFiles: List<ObjectFile>,
        val dependenciesTrackingResult: DependenciesTrackingResult,
        val outputFiles: OutputFiles,
        val resolvedCacheBinaries: ResolvedCacheBinaries,
        val isCoverageEnabled: Boolean,
)

internal val LinkerPhase = createSimpleNamedCompilerPhase<PhaseContext, LinkerPhaseInput>(
        name = "Linker",
        description = "Linker"
) { context, input ->
    val linker = Linker(
            config = context.config,
            linkerOutput = input.outputKind,
            isCoverageEnabled = input.isCoverageEnabled,
            outputFiles = input.outputFiles
    )
    val commands = linker.linkCommands(
            input.outputFile,
            input.objectFiles,
            input.dependenciesTrackingResult,
            input.resolvedCacheBinaries
    )
    runLinkerCommands(context, commands, cachingInvolved = !input.resolvedCacheBinaries.isEmpty())
}

internal data class PreLinkCachesInput(
        val objectFiles: List<File>,
        val caches: ResolvedCacheBinaries,
        val outputObjectFile: File,
)

internal val PreLinkCachesPhase = createSimpleNamedCompilerPhase<PhaseContext, PreLinkCachesInput>(
        name = "PreLinkCaches",
        description = "Pre-link static caches",
) { context, input ->
    val inputFiles = input.objectFiles.map { it.canonicalPath } + input.caches.static
    val commands = context.config.platform.linker.preLinkCommands(inputFiles, input.outputObjectFile.canonicalPath)
    runLinkerCommands(context, commands, cachingInvolved = true)
}