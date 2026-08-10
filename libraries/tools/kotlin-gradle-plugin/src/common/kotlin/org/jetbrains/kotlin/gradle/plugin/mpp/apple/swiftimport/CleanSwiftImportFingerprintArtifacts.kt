/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class CleanSwiftImportFingerprintArtifacts : DefaultTask() {

    @get:Internal
    abstract val syntheticPackageFingerprint: RegularFileProperty

    @get:Internal
    abstract val xcodebuildFingerprints: ConfigurableFileCollection

    @get:Internal
    abstract val coordinationService: Property<SwiftImportFingerprintedCoordinationService>

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun clean() {
        val pathsToDelete = mutableSetOf<File>()
        val coordinationService = coordinationService.get()

        readIncrementalFingerprint(syntheticPackageFingerprint.asFile.orNull)?.let { fingerprint ->
            pathsToDelete += coordinationService.sharedPackageGenerationRoot(fingerprint)
            pathsToDelete += coordinationService.sharedCheckoutDir(fingerprint)
        }

        xcodebuildFingerprints.files.forEach { fingerprintFile ->
            readIncrementalFingerprint(fingerprintFile)?.let { fingerprint ->
                pathsToDelete += coordinationService.sharedXcodeDumpBucketRoot(fingerprint)
            }
        }

        if (pathsToDelete.isNotEmpty()) {
            fileSystemOperations.delete {
                it.delete(pathsToDelete)
            }
        }
    }

    private fun readIncrementalFingerprint(fingerprintFile: File?): String? =
        fingerprintFile
            ?.takeIf(File::isFile)
            ?.readSwiftImportFingerprint()
            ?.incrementalFingerprint

    companion object {
        const val TASK_NAME = "cleanSwiftImportFingerprintArtifacts"
    }
}
