/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ib

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.commonizer.api.CommonizerTarget
import org.jetbrains.kotlin.commonizer.api.HierarchicalCommonizerOutputLayout.getTargetDirectory
import org.jetbrains.kotlin.commonizer.api.identityString
import java.io.File
import java.io.Serializable

abstract class CommonizerOutputSelectionTransformation : TransformAction<CommonizerOutputSelectionTransformation.Parameters> {
    open class Parameters : TransformParameters, Serializable {
        @Input
        var target: CommonizerTarget? = null
    }

    @get:InputArtifact
    abstract val commonizerOutput: Provider<FileSystemLocation>

    override fun transform(output: TransformOutputs) {
        val target = parameters.target ?: error("Missing target")

        val commonizerOutputDirectory = commonizerOutput.get().asFile
        check(commonizerOutputDirectory.isDirectory)

        val targetDirectory = getTargetDirectory(commonizerOutputDirectory, target)
        check(targetDirectory.isDirectory) { "Missing commonizer target $target" }

        targetDirectory.listFiles().orEmpty().forEach { targetLibraryDirectory ->
            check(targetLibraryDirectory.isDirectory)
            val outputDirectory = output.dir(File(target.identityString).resolve(targetLibraryDirectory.name))
            targetLibraryDirectory.copyRecursively(outputDirectory)
        }
    }
}
