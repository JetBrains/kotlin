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
import org.gradle.api.tasks.InputFile
import org.jetbrains.kotlin.commonizer.api.CliCommonizer
import org.jetbrains.kotlin.commonizer.api.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.ib.InteropBundleCommonizerTransformation.Parameters
import java.io.File
import java.io.Serializable


abstract class InteropBundleCommonizerTransformation : TransformAction<Parameters> {
    open class Parameters : TransformParameters, Serializable {

        @InputFile
        var konanHome: File? = null

        @Input
        var outputHierarchy: SharedCommonizerTarget? = null

        @Classpath
        var commonizerClasspath: Set<File>? = null
    }

    @get:Classpath
    @get:InputArtifact
    abstract val interopBundle: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val konanHome = parameters.konanHome ?: error("Missing konanHome")
        val outputHierarchy = parameters.outputHierarchy ?: error("Missing outputHierarchy")
        val commonizerClasspath = parameters.commonizerClasspath ?: error("Missing commonizerClasspath")

        val commonizer = CliCommonizer(commonizerClasspath)
        commonizer(
            konanHome = konanHome,
            targetLibraries = klibs(),
            dependencyLibraries = emptySet(),
            outputHierarchy = outputHierarchy,
            outputDirectory = outputs.dir("commonized")
        )
    }

    private fun klibs(): Set<File> {
        return interopBundle.get().asFile.walkTopDown().maxDepth(2)
            .filter { file -> file.isFile && file.extension == "klib" }
            .toSet()
    }
}


