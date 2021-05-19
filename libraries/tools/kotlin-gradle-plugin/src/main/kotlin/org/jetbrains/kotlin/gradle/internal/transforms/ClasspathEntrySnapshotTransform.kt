/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.transforms

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile.Companion.CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE

/** Transform to create a snapshot ([CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE]) of a classpath entry (directory or jar). */
@CacheableTransform
abstract class ClasspathEntrySnapshotTransform : TransformAction<TransformParameters.None> {

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val classpathEntry = inputArtifact.get().asFile
        val snapshotFile = outputs.file(CLASSPATH_ENTRY_SNAPSHOT_FILE_NAME)

        // TODO WORK-IN-PROGRESS
        val snapshot = "[Place holder - actual snapshot will be inserted in an upcoming change]"
        snapshotFile.writeText(snapshot)
    }
}

const val CLASSPATH_ENTRY_SNAPSHOT_FILE_NAME = "classpath-entry-snapshot.bin"
