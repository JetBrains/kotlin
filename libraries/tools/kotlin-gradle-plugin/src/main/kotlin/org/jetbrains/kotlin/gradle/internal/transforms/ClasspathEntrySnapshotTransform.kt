/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.transforms

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.jetbrains.kotlin.gradle.incremental.ClasspathEntrySnapshotSerializer
import org.jetbrains.kotlin.gradle.incremental.ClasspathEntrySnapshotter

/** Transform to create a snapshot of a classpath entry (directory or jar). */
@CacheableTransform
abstract class ClasspathEntrySnapshotTransform : TransformAction<TransformParameters.None> {

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val classpathEntry = inputArtifact.get().asFile
        val snapshotFile = outputs.file(classpathEntry.name.replace('.', '_') + "-snapshot.bin")

        val snapshot = ClasspathEntrySnapshotter.snapshot(classpathEntry)
        ClasspathEntrySnapshotSerializer.save(snapshotFile, snapshot)
    }
}
