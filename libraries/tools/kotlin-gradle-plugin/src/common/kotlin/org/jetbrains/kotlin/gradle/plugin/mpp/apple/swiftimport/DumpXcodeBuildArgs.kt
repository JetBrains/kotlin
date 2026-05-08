/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import javax.inject.Inject

/**
 * Dumps the clang and linker invocations that Xcode would use for the synthetic SwiftPM import project.
 *
 * Every project/SDK still owns a local instance of this task. Matching tasks coordinate through
 * [SwiftPMXcodeDumpBuildService]: exactly one owner runs the expensive xcodebuild command into the shared root-build
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

    /** File produced by [ComputeLocalPackageDependencyInputFiles], listing local package files/directories to track. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    /**
     * Local package source inputs. These are declared here so Gradle reruns the dump task when local package sources
     * change, even if Package.resolved did not change.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val localPackageSources: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map {
            it.asFile.readLines().filter { line -> line.isNotEmpty() }.map { line -> File(line) }
        }

    /** Hash produced by PrepareXcodeBuildArgsDumpFingerprint and read during execution to claim/join a bucket. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xcodebuildExecutionHashFile: RegularFileProperty

    /** Local marker consumed by ConvertSyntheticSwiftPMImportProjectIntoDefFile. */
    @get:OutputFile
    abstract val xcodeDumpLocationFile: RegularFileProperty

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
    val additionalXcodeArgs: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(emptyList())

    /** Checkout path passed to xcodebuild when this task owns the shared dump. */
    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    /** Synthetic SwiftPM project root passed to xcodebuild when this task owns the shared dump. */
    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val coordinationService: Property<SwiftPMXcodeDumpBuildService>

    @TaskAction
    fun dumpXcodeBuildArgs() {
        if (hasSwiftPMDependencies.get()) {
            // Fingerprints are calculated by a separate task because their inputs are generated during execution.
            // Reading the prepared file here keeps providers pure and avoids configuration-time claiming/rerouting.
            val xcodebuildExecutionHash = xcodebuildExecutionHashFile.get().asFile.readText()

            // The service decides whether this task owns the expensive xcodebuild execution or can reuse an existing
            // bucket from another task in this invocation or from a validated root-build bucket left by an earlier run.
            val claim = coordinationService.get().claimOrJoinXcodeDump(
                xcodebuildExecutionHash = xcodebuildExecutionHash,
                xcodebuildSdk = xcodebuildSdk.get(),
                sdkDerivedDataDirName = "dd_${xcodebuildSdk.get()}",
            )

            when (claim) {
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Owner -> runOwnerXcodeDump(claim.bucket)
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Existing -> coordinationService.get().awaitXcodeDump(claim.bucket)
            }

            writeXcodeDumpLocation(claim.bucket, xcodebuildExecutionHash)
        }
    }

    private fun runOwnerXcodeDump(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        try {
            // The owner writes directly to the root-build bucket, while still building this task's synthetic package and
            // using this task's SwiftPM checkout.
            submitXcodebuildArgsDumpWorkAction(
                ownerDumpDir = bucket.ownerDumpDir,
                ownerDerivedDataDir = bucket.ownerDerivedDataDir,
            )
            workerExecutor.await()
            // Completion stamps the shared dump and releases any tasks waiting on the same bucket.
            coordinationService.get().markXcodeDumpCompleted(bucket)
        } catch (failure: Throwable) {
            // Propagate the same failure to every task that joined this bucket.
            coordinationService.get().markXcodeDumpFailed(bucket, failure)
            throw failure
        }
    }

    private fun submitXcodebuildArgsDumpWorkAction(
        ownerDumpDir: File,
        ownerDerivedDataDir: File,
    ) {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            // Most parameters still come from the local task. Only the output locations are overridden with the bucket
            // owner directories so shared execution writes exactly where waiters will read from.
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.fileValue(ownerDerivedDataDir)
            params.dumpedXcodeBuildArgsDir.fileValue(ownerDumpDir)
            params.additionalXcodeArgs.set(additionalXcodeArgs)
        }
    }

    private fun writeXcodeDumpLocation(
        bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket,
        xcodebuildExecutionHash: String,
    ) {
        val sdkDerivedDataDir = "dd_${xcodebuildSdk.get()}"
        val locationFile = xcodeDumpLocationFile.get().asFile
        locationFile.parentFile.mkdirs()
        locationFile.writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpLocation(
                    xcodebuildExecutionHash = xcodebuildExecutionHash,
                    dumpedXcodeBuildArgsDir = bucket.ownerDumpDir.path,
                    derivedDataDir = bucket.ownerDerivedDataDir.resolve(sdkDerivedDataDir).path,
                )
            )
        )
    }

    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
    }
}

@Serializable
internal data class XcodeDumpLocation(
    val xcodebuildExecutionHash: String,
    val dumpedXcodeBuildArgsDir: String,
    val derivedDataDir: String,
)
