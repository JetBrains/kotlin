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
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.api.identityString
import java.io.File
import java.io.Serializable

abstract class InteropBundlePlatformSelectionTransformation : TransformAction<InteropBundlePlatformSelectionTransformation.Parameters> {
    open class Parameters : TransformParameters, Serializable {
        @Input
        var target: LeafCommonizerTarget? = null
    }

    @get:Classpath
    @get:InputArtifact
    abstract val interopBundle: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val target = parameters.target ?: error("Missing target")
        val interopBundleFile = interopBundle.get().asFile
        val platformLibraryFiles = interopBundleFile.resolve(target.identityString)
        for (platformLibraryFile in platformLibraryFiles.listFiles().orEmpty()) {
            val outputFile = outputs.file(File(target.identityString).resolve(platformLibraryFile.name))
            platformLibraryFile.copyTo(outputFile)
        }
    }
}
