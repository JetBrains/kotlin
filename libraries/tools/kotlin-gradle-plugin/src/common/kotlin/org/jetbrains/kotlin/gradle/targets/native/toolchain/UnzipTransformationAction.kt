/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject


private const val EXTRACTED_ARCHIVE_RELATED_PATH = "extracted"

/**
 * An implementation of a gradle [TransformAction] to unzip configurations' artifacts in `tar.gz` and `zip` formats.
 */
@DisableCachingByDefault(because = "Does only I/O")
abstract class UnzipTransformationAction : TransformAction<TransformParameters.None> {

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val unzipDir = outputs.dir(EXTRACTED_ARCHIVE_RELATED_PATH)
        unzipTo(input, unzipDir)
    }

    private fun unzipTo(archive: File, outputDir: File) {
        fileSystemOperations.copy {
            it.from(
                when {
                    archive.name.endsWith("zip") -> archiveOperations.zipTree(archive)
                    archive.name.endsWith(".tar.gz") -> archiveOperations.tarTree(archive)
                    else -> error("Unsupported format for unzipping $archive")
                }
            )
            it.into(outputDir)
        }
    }
}