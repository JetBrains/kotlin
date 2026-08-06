/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.decodeFromString
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

internal abstract class SwiftImportFingerprintInput {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fingerprintFile: RegularFileProperty

    /**
     * Wired only for synthetic package fingerprints whose owning task reads the shared coordination buckets.
     *
     * It is deliberately left unset for xcodebuild fingerprints - their hash keys a different, SDK scoped bucket - and
     * when coordination is disabled. In both cases the checks below are no-ops.
     */
    @get:Internal
    abstract val coordinationService: Property<SwiftImportFingerprintedCoordinationService>

    /**
     * The shared synthetic package for this fingerprint lives under the root project's build directory and is consumed
     * by tasks in other projects, so it cannot be declared as a task output: every coordinated task would then declare
     * the same overlapping output. Tracking it as an input instead invalidates the task once the bucket is gone, for
     * example after the root build directory is removed. See KT-88104.
     */
    @get:Input
    val sharedGeneratedPackageExists: Boolean
        get() = checkSharedOutputs { sharedPackageGenerationOutputsExist(it) }

    /** As [sharedGeneratedPackageExists], for the shared `Package.resolved` and the shared SwiftPM checkout. */
    @get:Input
    val sharedResolvedPackagesExist: Boolean
        get() = checkSharedOutputs { sharedSwiftResolveOutputsExist(it) }

    private fun checkSharedOutputs(
        check: SwiftImportFingerprintedCoordinationService.(packageHash: String) -> Boolean,
    ): Boolean {
        val coordinationService = coordinationService.orNull ?: return true
        val packageHash = readFingerprintOrNull()?.incrementalFingerprint ?: return true
        return coordinationService.check(packageHash)
    }

    fun readFingerprint(): SwiftImportFingerprint =
        fingerprintFile.get().asFile.readSwiftImportFingerprint()

    fun readFingerprintOrNull(): SwiftImportFingerprint? =
        fingerprintFile.asFile.orNull
            ?.takeIf(File::isFile)
            ?.readSwiftImportFingerprint()

}

internal fun File.readSwiftImportFingerprint(): SwiftImportFingerprint =
    fingerprintJson.decodeFromString<SwiftImportFingerprint>(readText())

internal abstract class LocalPackageTrackingInputs {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val nonEmptyFilesFromLocalPackages: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map { inputFile ->
            inputFile.asFile
                .readLines()
                .filter { it.isNotBlank() }
                .map(::File)
        }
}
