/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME
import java.util.zip.ZipFile

/**
 * Platform compile dependency configurations can inherit dependencies from metadata variant. We remove the resolved metadata jars here
 */
@CacheableTransform
internal abstract class ThrowAwayMetadataJarsTransform : TransformAction<TransformParameters.None> {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val jar = inputArtifact.get().asFile
        // Sanity check
        if (jar.extension != "jar") {
            // Just return whatever this is
            outputs.file(jar)
            return
        }

        val isMetadataJar: Boolean = ZipFile(jar).use { zip ->
            zip.entries().asSequence().any {
                it.name.endsWith("META-INF/$MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME")
            }
        }
        // Return nothing on metadata jar
        if (isMetadataJar) return
        outputs.file(jar)
    }
}