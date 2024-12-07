/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

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
import org.jetbrains.kotlin.gradle.plugin.mpp.EMPTY_PROJECT_STRUCTURE_METADATA_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import javax.inject.Inject

@DisableCachingByDefault(because = "Trivial transformation: does only I/O operations.")
abstract class ProjectStructureMetadataTransformAction : TransformAction<TransformParameters.None> {

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val psm = archiveOperations.zipTree(input).matching { it.include("META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME") }
            .singleOrNull()
        if (psm != null) {
            val outputFile = outputs.file(MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME)
            fileSystemOperations.copy {
                it.from(psm)
                it.into(outputFile.parentFile)
                it.rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
            }
        } else {
            outputs.file(EMPTY_PROJECT_STRUCTURE_METADATA_FILE_NAME).createNewFile()
        }
    }

}