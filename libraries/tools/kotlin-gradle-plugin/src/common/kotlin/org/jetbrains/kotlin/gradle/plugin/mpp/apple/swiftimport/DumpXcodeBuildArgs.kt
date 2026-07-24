/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
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
    @get:Internal
    abstract val syntheticPackageFingerprint: RegularFileProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val fingerprintCoordinationService: Property<SwiftImportFingerprintedCoordinationService>

    @get:Internal
    abstract val testExecutionHooks: Property<SwiftImportExecutionHooks>

    @get:Internal
    abstract val testExecutionService: Property<SwiftImportTestExecutionService>

    private val layout = project.layout

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Nested
    abstract val localPackages: LocalPackageTrackingInputs

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

    /**
     * When `true` (IDE sync) an xcodebuild failure is downgraded to a warning + stub outputs
     * instead of failing the IDE import. See KT-85468.
     */
    @get:Input
    abstract val ideaSyncEnabled: Property<Boolean>


    /**
     * Written only when xcodebuild fails leniently during IDE sync, so the task is not considered
     * up-to-date and the next regular build retries. Mirrors CInteropProcess.errorFileProvider.
     */
    @get:OutputFile
    val ideImportError: Provider<RegularFile> = syntheticImportDd.map {
        it.file("DumpXcodebuild_error_${xcodebuildSdk.get()}.out")
    }

    private val xcodebuildFinishedMarkerFile: File
        get() {
            val markerName = "xcodebuildFinishedMarker"
            if (isCoordinationDisabled()) {
                return localDerivedDataDir().resolve(markerName)
            } else {
                return fingerprintCoordinationService.get().sharedXcodeDerivedDataDir(
                    xcodebuildExecutionHash = readXcodebuildFingerprint(),
                    xcodebuildSdk = xcodebuildSdk.get(),
                ).resolve(markerName)
            }
        }

    private fun readXcodebuildFingerprint() = xcodebuildFingerprint.asFile.get().readText().trim().split("\n")[1]
    private fun localDerivedDataDir() = syntheticImportDd.get().asFile.resolve("dd_${xcodebuildSdk.get()}")

    @Suppress("SENSELESS_COMPARISON")
    private fun isCoordinationDisabled() = xcodebuildFingerprint.asFile.orNull == null || syntheticPackageFingerprint.asFile.orNull == null

    // ./gradlew clean
    init {
        testExecutionHooks.convention(SwiftImportExecutionHooks.NONE)
        outputs.upToDateWhen {
            xcodebuildFinishedMarkerFile.exists()
        }
        // KT-85468: while the error marker exists the task is not up-to-date so the next build retries.
        outputs.upToDateWhen { !ideImportError.get().asFile.exists() }
    }

    @TaskAction
    fun dumpXcodeBuildArgs() {
        val errorFile = ideImportError.get().asFile
        errorFile.delete()

        // this is the case when package sync strategy is set to PackageResolvedSynchronization.None
        if (isCoordinationDisabled()) {
            submitXcodebuildArgsDumpWorkAction(
                dumpDir = syntheticDumpDir.get().asFile,
                derivedDataDir = syntheticImportDd.get().asFile.resolve("dd_${xcodebuildSdk.get()}"),
                syntheticImportProjectRoot = syntheticImportProjectRoot.get().asFile,
                swiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get().asFile,
                xcodebuildExecutionHash = null,
            )
            return
        }

        val syntheticPackageFingerprintFile = syntheticPackageFingerprint.asFile.get()

        val coordinationService = fingerprintCoordinationService.get()

        val syntheticPackageFingerprint = syntheticPackageFingerprintFile.readText().trim().split("\n")[1]

        testExecutionHooks.get().beforeXcodebuildClaim()
        val claim = fingerprintCoordinationService.get().claimOrJoinXcodeDump(
            xcodebuildExecutionHash = readXcodebuildFingerprint(),
            xcodebuildSdk = xcodebuildSdk.get(),
        )
        when (claim) {
            is CoordinationClaim.Owner -> {
                testExecutionHooks.get().beforeXcodebuildOwnerWorkerSubmission()
                runOwnerXcodeDump(
                    dumpDir = claim.bucket.ownerDumpDir,
                    derivedDataDir = claim.bucket.ownerDerivedDataDir,
                    syntheticImportProjectRoot = coordinationService.sharedPackageGenerationRoot(syntheticPackageFingerprint),
                    swiftPMDependenciesCheckout = coordinationService.sharedCheckoutDir(syntheticPackageFingerprint),
                    xcodebuildExecutionHash = claim.bucket.key,
                )
            }

            is CoordinationClaim.Existing -> {
                coordinationService.awaitXcodeDumpOwnerStarted(claim.bucket.key)
                workerExecutor.noIsolation().submit(
                    XcodebuildArgsDumpAwaitWorkAction::class.java
                ) {
                    it.fingerprintCoordinationService.set(fingerprintCoordinationService)
                    it.key.set(claim.bucket.key)
                    it.ideaSyncEnabled.set(ideaSyncEnabled)
                    it.errorFile.set(errorFile)
                    it.testExecutionService.set(testExecutionService)
                }
            }
        }
    }

    private fun runOwnerXcodeDump(
        dumpDir: File,
        derivedDataDir: File,
        syntheticImportProjectRoot: File,
        swiftPMDependenciesCheckout: File,
        xcodebuildExecutionHash: XcodeDumpBucketMapKey,
    ) {
        submitXcodebuildArgsDumpWorkAction(
            dumpDir = dumpDir,
            derivedDataDir = derivedDataDir,
            syntheticImportProjectRoot = syntheticImportProjectRoot,
            swiftPMDependenciesCheckout = swiftPMDependenciesCheckout,
            xcodebuildExecutionHash = xcodebuildExecutionHash,
        )
    }

    private fun submitXcodebuildArgsDumpWorkAction(
        dumpDir: File,
        derivedDataDir: File,
        syntheticImportProjectRoot: File,
        swiftPMDependenciesCheckout: File,
        xcodebuildExecutionHash: XcodeDumpBucketMapKey?,
    ) {
        val isCoordinationEnabled = xcodebuildExecutionHash != null
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.fileValue(derivedDataDir)
            params.dumpedXcodeBuildArgsDir.fileValue(dumpDir)
            params.additionalXcodeArgs.set(additionalXcodeArgs)
            params.coordinationEnabled.set(isCoordinationEnabled)
            params.ideaSyncEnabled.set(ideaSyncEnabled)
            params.errorFile.set(ideImportError)
            params.xcodebuildFinishedMarkerFile.set(xcodebuildFinishedMarkerFile)
            params.testExecutionService.set(testExecutionService)

            if (isCoordinationEnabled) {
                params.fingerprintCoordinationService.set(fingerprintCoordinationService)
                params.xcodebuildExecutionFingerprint.set(xcodebuildExecutionHash!!)
            }
        }
    }


    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
    }
}
