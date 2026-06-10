/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import javax.inject.Inject

/**
 * Dumps the clang and linker invocations that Xcode would use for the synthetic SwiftPM import project.
 *
 * Every project/SDK still owns a local instance of this task. Matching tasks coordinate through
 * [SwiftImportFingerprintedCoordinationService]: exactly one owner runs the expensive xcodebuild command into the shared root-build
 * bucket, and every participant writes a local location file pointing downstream tasks to that bucket.
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class DumpXcodeBuildArgs : DefaultTask() {
    /** Xcode destination platform passed to xcodebuild, for example `iOS` or `iOS Simulator`. */
    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    /** SDK name used by xcodebuild and by the SDK-specific DerivedData directory name. */
    @get:Input
    abstract val xcodebuildSdk: Property<String>

    /** Architectures requested from xcodebuild. The generated dump files are architecture-specific. */
    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    /** Allows the task to stay registered for all projects while doing no work when the project has no SwiftPM graph. */
    @get:Input
    abstract val hasSwiftPMDependencies: Property<Boolean>

    /** Hash produced by PrepareXcodeBuildArgsDumpFingerprint and read during execution to claim/join a bucket. */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xcodebuildFingerprint: RegularFileProperty

    /**
     * Additional arguments to pass to `xcodebuild` when resolving SwiftPM dependencies.
     *
     * Generally used in test to:
     * To avoid cache collisions between test runs, we generate a unique package name (and therefore URL) for each execution.
     * e.g "Revision ... for TestPackageA version 1.0.0 does not match previously recorded value ..."
     * or
     * Optional SwiftPM repository cache override.
     * Passed to `xcodebuild` as:
     * -packageCachePath <dir>
     * Used in tests to avoid collisions with the global cache at `~/Library/Caches/org.swift.swiftpm/repositories`.
     */
    @get:Input
    val additionalXcodeArgs: ListProperty<String> = project.objects.listProperty(String::class.java).convention(emptyList())

    /** Checkout path passed to xcodebuild when this task owns the shared dump. */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val syntheticPackageFingerprint: RegularFileProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val fingerprintCoordinationService: Property<SwiftImportFingerprintedCoordinationService>

    private val layout = project.layout

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Internal
    val syntheticImportDd: Provider<Directory> =
        layout.buildDirectory.dir(XcodebuildDefFileUtils.SYNTHETIC_IMPORT_DD_DIR)

    @get:Internal
    val syntheticDumpDir: Provider<Directory> = xcodebuildSdk.map { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.clangDumpRelativeDir(sdk)).get()
    }


    @TaskAction
    fun dumpXcodeBuildArgs() {
        if (!hasSwiftPMDependencies.get()) return

        val coordinationService = fingerprintCoordinationService.get()

        val fetchBucket = syntheticPackageFingerprint.orNull?.asFile
            ?.readText()
            ?.trim()
            ?.let { syntheticPackageHash ->
                coordinationService.findSwiftResolveBucket(syntheticPackageHash)
                    ?: error("SwiftPM resolve bucket is missing for package hash $syntheticPackageHash")
            }

        if (fetchBucket != null) {
            coordinationService.awaitSwiftResolved(fetchBucket)
        }

        val xcodebuildFingerprintFile = xcodebuildFingerprint.orNull?.asFile
        if (xcodebuildFingerprintFile == null) {
            submitXcodebuildArgsDumpWorkAction(
                dumpDir = syntheticDumpDir.get().asFile,
                derivedDataDir = syntheticImportDd.get().asFile,
                syntheticImportProjectRoot = syntheticImportProjectRoot.get().asFile,
                swiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get().asFile,
            )
            return
        }

        val xcodebuildExecutionFingerprint = xcodebuildFingerprintFile.readText().trim()

        val claim = coordinationService.claimOrJoinXcodeDump(
            xcodebuildExecutionHash = xcodebuildExecutionFingerprint,
            xcodebuildSdk = xcodebuildSdk.get(),
        )

        when (claim) {
            is CoordinationClaim.Owner -> {
                requireNotNull(fetchBucket) {
                    "SwiftPM resolve bucket is required when owning xcodebuild args dump"
                }

                runOwnerXcodeDump(
                    dumpDir = claim.bucket.ownerDumpDir,
                    derivedDataDir = claim.bucket.ownerDerivedDataDir,
                    syntheticImportProjectRoot = fetchBucket.ownerSyntheticImportProjectRoot,
                    swiftPMDependenciesCheckout = fetchBucket.ownerSwiftPMDependenciesCheckout,
                    xcodebuildExecutionHash = xcodebuildExecutionFingerprint,
                )
            }

            is CoordinationClaim.Existing -> {
                coordinationService.awaitXcodeDump(claim.bucket)
            }
        }
    }

    private fun runOwnerXcodeDump(
        dumpDir: File,
        derivedDataDir: File,
        syntheticImportProjectRoot: File,
        swiftPMDependenciesCheckout: File,
        xcodebuildExecutionHash: String,
    ) {
        submitXcodebuildArgsDumpWorkAction(
            dumpDir = dumpDir,
            derivedDataDir = derivedDataDir,
            syntheticImportProjectRoot = syntheticImportProjectRoot,
            swiftPMDependenciesCheckout = swiftPMDependenciesCheckout,
            xcodebuildExecutionHash = xcodebuildExecutionHash,
            markCompletion = true,
        )
    }

    private fun submitXcodebuildArgsDumpWorkAction(
        dumpDir: File,
        derivedDataDir: File,
        syntheticImportProjectRoot: File,
        swiftPMDependenciesCheckout: File,
        xcodebuildExecutionHash: String? = null,
        markCompletion: Boolean = false,
    ) {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.fileValue(derivedDataDir)
            params.dumpedXcodeBuildArgsDir.fileValue(dumpDir)
            params.additionalXcodeArgs.set(additionalXcodeArgs)
            params.markCompletion.set(markCompletion)

            if (markCompletion) {
                params.fingerprintCoordinationService.set(fingerprintCoordinationService)
                params.xcodebuildExecutionFingerprint.set(xcodebuildExecutionHash!!)
            }
        }
    }

    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
    }
}
