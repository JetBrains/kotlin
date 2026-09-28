/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal data class LinkerPhaseInput(
        val outputFile: String,
        val outputKind: LinkerOutputKind,
        val objectFiles: List<ObjectFile>,
        val dependenciesTrackingResult: DependenciesTrackingResult,
        val outputFiles: OutputFiles,
        val tempFiles: TempFiles,
        val resolvedCacheBinaries: ResolvedCacheBinaries,
        val extraLinkerFlags: List<String> = emptyList()
)

internal val LinkerPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, LinkerPhaseInput>(
        name = "Linker",
) { context, input ->
    val linker = Linker(
            config = context.config,
            linkerOutput = input.outputKind,
            outputFiles = input.outputFiles,
            tempFiles = input.tempFiles,
    )
    val commands = linker.linkCommands(
            input.outputFile,
            input.objectFiles,
            input.dependenciesTrackingResult,
            input.resolvedCacheBinaries,
            input.extraLinkerFlags
    )
    runLinkerCommands(context, commands, cachingInvolved = !input.resolvedCacheBinaries.isEmpty())
}

internal data class PreLinkCachesInput(
        val objectFiles: List<Path>,
        val caches: ResolvedCacheBinaries,
        val outputObjectFile: Path,
)

internal val PreLinkCachesPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, PreLinkCachesInput>(
        name = "PreLinkCaches",
) { context, (val objectFiles, val caches, val outputObjectFile) ->
    val inputFiles = objectFiles.map { it.absolutePathString() } + caches.static
    val commands = context.config.platform.linker.preLinkCommands(inputFiles, outputObjectFile.absolutePathString())
    runLinkerCommands(context, commands, cachingInvolved = true)
}
