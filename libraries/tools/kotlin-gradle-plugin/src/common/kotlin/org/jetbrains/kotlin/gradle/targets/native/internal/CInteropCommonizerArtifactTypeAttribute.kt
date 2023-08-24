/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerArtifactTypeAttribute.KLIB
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerArtifactTypeAttribute.KLIB_COLLECTION_DIR

/**
 * Commonized CInterop artifacts can come in two shapes:
 * [KLIB]: Just a 'klib' directory or packaged file representing one commonized library
 * [KLIB_COLLECTION_DIR]: A flat directory containing several klib dirs or files representing the output of a commonizer task
 */
internal object CInteropCommonizerArtifactTypeAttribute {
    val attribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.cinteropCommonizerArtifactType", String::class.java)

    /**
     * 'Regular'/'Typical' klib in the form of a 'directory' or '.klib' file representing a single library
     */
    const val KLIB = "klib"

    /**
     * Directory containing klibs as direct children:
     * e.g. output
     *
     * ```
     * build/some/output/
     *    - foo.klib
     *    - bar.klib
     *    - ...
     * ```
     *
     * In this case the artifact will just be the 'output' directory that contains the collection of klibs
     */
    const val KLIB_COLLECTION_DIR = "klib-collection-dir"

    /**
     * Set up a transformation from artifacts of type 'collection dir' to a set of klibs.
     */
    fun setupTransform(project: Project) {
        project.dependencies.artifactTypes.maybeCreate(KLIB_COLLECTION_DIR).also { artifactType ->
            artifactType.attributes.attribute(attribute, KLIB_COLLECTION_DIR)
        }

        project.dependencies.artifactTypes.maybeCreate(KLIB).also { artifactType ->
            artifactType.attributes.attribute(attribute, KLIB)
        }

        project.dependencies.registerTransform(KlibCollectionDirTransform::class.java) { transform ->
            transform.from.attribute(attribute, KLIB_COLLECTION_DIR)
            transform.to.attribute(attribute, KLIB)
        }
    }

    @DisableCachingByDefault(because = "Trivial operation, building cache keys is presumably more expensive")
    abstract class KlibCollectionDirTransform : TransformAction<TransformParameters.None> {

        @get:InputArtifact
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val inputArtifact: Provider<FileSystemLocation>

        override fun transform(outputs: TransformOutputs) {
            val input = inputArtifact.get().asFile
            input.listFiles()?.forEach { klib ->
                if (klib.isDirectory) outputs.dir(klib)
                if (klib.isFile) outputs.file(klib)
            }
        }
    }
}