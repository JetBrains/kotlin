/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.wasm.component.WIT_DIRECTORY_NAME
import java.io.File
import javax.inject.Inject

/**
 * Extracts the top-level `wit` directory of a klib into a standalone directory.
 *
 * The resulting directory contains the contents of the `wit` directory of the klib,
 * klibs without such a directory produce no output at all.
 */
@DisableCachingByDefault(because = "Trivial transformation: does only I/O operations")
internal abstract class WitExtractionTransform @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val klib = inputArtifact.get().asFile

        if (klib.isDirectory) {
            val witDirectory = klib.resolve(WIT_DIRECTORY_NAME)
            if (!witDirectory.isDirectory) return

            fileOperations.copy {
                it.from(witDirectory)
                it.into(outputs.dir(outputDirectoryName(klib)))
            }

            return
        }

        val witFiles = archiveOperations.zipTree(klib)
            .matching { it.include("$WIT_DIRECTORY_NAME/**") }

        if (witFiles.isEmpty) return

        fileOperations.copy { copy ->
            copy.from(witFiles) { wit ->
                wit.includeEmptyDirs = false
                wit.eachFile { file ->
                    file.relativePath = RelativePath(
                        true,
                        *file.relativePath.segments.drop(1).toTypedArray()
                    )
                }
            }
            copy.into(outputs.dir(outputDirectoryName(klib)))
        }
    }

    private fun outputDirectoryName(klib: File): String =
        "${klib.name}-$WIT_DIRECTORY_NAME"
}

internal const val KOTLIN_WIT_ARTIFACT = "kotlin-wasm-wit"
