/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import javax.inject.Inject

internal abstract class UnzipUklibTransform @Inject constructor(
    private val fileOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : TransformAction<UnzipUklibTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val performUnzip: Property<Boolean>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        // Due to kotlin-api/runtime Usages being compatible with java Usages, we might see a jar in this transform
        if (input.extension == Uklib.UKLIB_EXTENSION) {
            val outputDir = outputs.dir("unzipped_uklib_${input.name}")
            if (parameters.performUnzip.get()) {
                fileOperations.copy {
                    it.from(archiveOperations.zipTree(inputArtifact.get().asFile))
                    it.into(outputDir)
                }
            }
        } else {
            error("???")
        }
    }
}