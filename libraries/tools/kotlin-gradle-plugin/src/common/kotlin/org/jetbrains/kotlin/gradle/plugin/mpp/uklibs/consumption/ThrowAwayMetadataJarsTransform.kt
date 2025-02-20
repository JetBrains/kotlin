/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.util.zip.ZipFile

@DisableCachingByDefault(because = "Investigate caching uklib transforms")
internal abstract class ThrowAwayMetadataJarsTransform : TransformAction<ThrowAwayMetadataJarsTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        // FIXME: Fake metadata jar?
        val fakeUklibUnzip: Property<Boolean>
    }

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

        val isMetadataJar: Boolean = if (parameters.fakeUklibUnzip.get())
            false
        else ZipFile(jar).use { zip ->
            zip.entries().asSequence().any {
                it.name.endsWith("kotlin-project-structure-metadata.json")
            }
        }
        // Return nothing on metadata jar
        if (isMetadataJar) return
        outputs.file(jar)
    }
}