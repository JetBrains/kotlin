/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Dumps the clang and linker invocations that Xcode would use for the synthetic SwiftPM import project.
 *
 * Every project/SDK still owns a local instance of this task and a local [dumpedXcodeBuildArgsDir]. Sharing happens
 * only inside the task action: matching tasks coordinate through [SwiftPMXcodeDumpBuildService], exactly one owner runs
 * the expensive xcodebuild command, and every participant materializes equivalent local outputs for downstream tasks.
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

    /** Local task output consumed by ConvertSyntheticSwiftPMImportProjectIntoDefFile. */
    @get:OutputDirectory
    abstract val dumpedXcodeBuildArgsDir: DirectoryProperty

    /**
     * Local DerivedData root. The owner writes its xcodebuild DerivedData here; loser tasks copy the SDK-specific
     * subdirectory from the owner so later local link/cinterop steps can reference their own project build directory.
     */
    @get:OutputDirectory
    val syntheticImportDd: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir(XcodebuildDefFileUtils.SYNTHETIC_IMPORT_DD_DIR)
    )

    /** JSON produced by PrepareXcodeBuildArgsDumpFingerprint and read during execution to claim/join a bucket. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fingerprintsFile: RegularFileProperty

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

    /** Checkout path passed to xcodebuild. It is remapped when another project owns the shared dump. */
    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    /** Synthetic SwiftPM project root passed to xcodebuild. It is remapped when another project owns the shared dump. */
    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Inject
    protected abstract val fs: FileSystemOperations

    @get:Internal
    abstract val coordinationService: Property<SwiftPMXcodeDumpBuildService>

    @TaskAction
    fun dumpXcodeBuildArgs() {
        if (hasSwiftPMDependencies.get()) {
            // Fingerprints are calculated by a separate task because their inputs are generated during execution.
            // Reading the prepared file here keeps providers pure and avoids configuration-time claiming/rerouting.
            val fingerprints = dumpTaskFingerprintJson.decodeFromString<XcodeDumpSharingFingerprints>(
                fingerprintsFile.get().asFile.readText()
            )

            // The service decides whether this task owns the expensive xcodebuild execution or can reuse an existing
            // bucket from another task/current invocation/persisted previous invocation.
            val claim = coordinationService.get().claimOrJoinXcodeDump(
                packageResolvedHash = fingerprints.packageResolvedHash,
                identifierDepsHash = fingerprints.identifierDepsHash,
                sdkDerivedDataDirName = "dd_${xcodebuildSdk.get()}",
                ownerDumpDir = dumpedXcodeBuildArgsDir.get().asFile,
                ownerDerivedDataDir = syntheticImportDd.get().asFile,
                syntheticImportProjectRoot = syntheticImportProjectRoot.get().asFile,
                swiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get().asFile,
            )

            // At this point the Gradle task graph is fixed, so non-owners cannot add a dependency on the owner task.
            // They wait on the bucket latch instead, then copy the owner outputs into their own declared outputs.
            when (claim) {
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Owner -> runOwnerXcodeDump(claim.bucket)
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Existing -> coordinationService.get().awaitXcodeDump(claim.bucket)
            }

            // Downstream tasks remain local and stable: ConvertSyntheticSwiftPMImportProjectIntoDefFile still consumes
            // this task's own output directory, regardless of which task actually ran xcodebuild.
            copyOwnerDumpToLocalOutput(claim.bucket)
            copyOwnerDerivedDataToLocalOutput(claim.bucket)
        }
    }

    private fun runOwnerXcodeDump(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        try {
            deleteCopiedDerivedDataBeforeOwnerRun(bucket)
            // The owner writes directly to the bucket's owner directories, which are normally this task's local outputs.
            submitXcodebuildArgsDumpWorkAction(
                ownerDumpDir = bucket.ownerDumpDir,
                ownerDerivedDataDir = bucket.ownerDerivedDataDir,
            )
            workerExecutor.await()
            // Completion stores the persisted state and releases any tasks waiting on the same bucket.
            coordinationService.get().markXcodeDumpCompleted(bucket)
        } catch (failure: Throwable) {
            // Propagate the same failure to every loser that joined this bucket.
            coordinationService.get().markXcodeDumpFailed(bucket, failure)
            throw failure
        }
    }

    private fun deleteCopiedDerivedDataBeforeOwnerRun(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        val sdkDerivedDataDir = "dd_${xcodebuildSdk.get()}"
        val ownerDerivedDataDir = bucket.ownerDerivedDataDir.resolve(sdkDerivedDataDir)
        val copiedDerivedDataMarker = ownerDerivedDataDir.resolve(COPIED_DERIVED_DATA_MARKER)
        if (!copiedDerivedDataMarker.exists()) return

        // A project can first be a loser and materialize another owner's DerivedData locally, then become the owner in a
        // later Gradle invocation after the original owner is cleaned or no longer matches. Copied DerivedData can carry
        // Xcode build database files with absolute paths to the old owner. We only remap the dumped clang/linker args,
        // not opaque DerivedData internals, because DerivedData contains binary/build database/cache files that are not
        // safe to text-rewrite. Running xcodebuild on top of that state can produce "stale file is located outside of
        // the allowed root paths" warnings and fail. Start clean in that case.
        fs.delete {
            it.delete(ownerDerivedDataDir)
        }
    }

    private fun submitXcodebuildArgsDumpWorkAction(
        ownerDumpDir: File,
        ownerDerivedDataDir: File,
    ) {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            // Most parameters still come from the local task. Only the output locations are overridden with the bucket
            // owner directories so shared execution writes exactly where waiters will copy from.
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

    /**
     * Materializes the owner dump into this task's local output directory.
     *
     * The copy is skipped for the owner because xcodebuild already wrote to its local directory. For loser tasks this
     * preserves Gradle's normal producer/consumer wiring: downstream tasks do not need to know which project won the
     * shared execution.
     */
    private fun copyOwnerDumpToLocalOutput(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        val localDumpDir = dumpedXcodeBuildArgsDir.get().asFile
        val ownerDumpDir = bucket.ownerDumpDir
        if (localDumpDir.canonicalFile == ownerDumpDir.canonicalFile) return

        fs.delete {
            it.delete(localDumpDir)
        }
        fs.copy {
            it.from(ownerDumpDir)
            it.into(localDumpDir)
        }
        remapOwnerLocalPaths(localDumpDir, bucket)
    }

    /**
     * Copies the SDK-specific DerivedData produced by the owner into this task's local DerivedData root.
     *
     * The dumped linker/clang args can reference products inside DerivedData. Keeping a local copy avoids leaking owner
     * project build paths into later link/cinterop work and also makes the local task output self-contained.
     *
     * TODO: Discuss with Timofey whether excluding CompilationCache.noindex is the right long-term contract. I saw
     * loser materialization copy the owner's tiny-looking CompilationCache into huge regular files (for example 44K in
     * the owner becoming 24G in the loser), which makes the task look stuck and consumes unnecessary disk space. The
     * dumped invocations should not reference this Xcode cache directory, so exclude it for now.
     */
    private fun copyOwnerDerivedDataToLocalOutput(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        val sdkDerivedDataDir = "dd_${xcodebuildSdk.get()}"
        val localDerivedDataDir = syntheticImportDd.get().asFile.resolve(sdkDerivedDataDir)
        val ownerDerivedDataDir = bucket.ownerDerivedDataDir.resolve(sdkDerivedDataDir)
        if (localDerivedDataDir.canonicalFile == ownerDerivedDataDir.canonicalFile) return

        fs.delete {
            it.delete(localDerivedDataDir)
        }
        fs.copy {
            it.from(ownerDerivedDataDir)
            it.into(localDerivedDataDir)
            it.exclude("CompilationCache.noindex/**")
        }
        localDerivedDataDir.resolve(COPIED_DERIVED_DATA_MARKER).writeText(
            "Copied from ${bucket.ownerDerivedDataDir.resolve(sdkDerivedDataDir).path}"
        )
    }

    /**
     * Rewrites owner-local absolute paths inside copied dump files.
     *
     * xcodebuild emits absolute paths to the synthetic project, SwiftPM checkout, and DerivedData. When a loser task
     * copies another project's dump, those paths must point at the loser's local directories; otherwise cinterop/link
     * outputs would depend on the owner project and can pick the wrong platform products after cross-project reuse.
     * This intentionally applies only to dump files, not copied DerivedData internals. If a loser later becomes owner,
     * the copied DerivedData is deleted before xcodebuild runs instead of attempting to remap Xcode's internal state.
     */
    private fun remapOwnerLocalPaths(
        localDumpDir: File,
        bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket,
    ) {
        val sdkDerivedDataDir = "dd_${xcodebuildSdk.get()}"
        val replacements = listOf(
            bucket.ownerSyntheticImportProjectRoot.path to syntheticImportProjectRoot.get().asFile.path,
            bucket.ownerSwiftPMDependenciesCheckout.path to swiftPMDependenciesCheckout.get().asFile.path,
            bucket.ownerDerivedDataDir.resolve(sdkDerivedDataDir).path to syntheticImportDd.get().asFile.resolve(sdkDerivedDataDir).path,
        ).filter { (from, to) -> from != to }

        if (replacements.isEmpty()) return

        localDumpDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val original = file.readText()
                val remapped = replacements.fold(original) { content, (from, to) ->
                    content.replacePath(from, to)
                }
                if (remapped != original) file.writeText(remapped)
            }
    }

    private fun String.replacePath(from: String, to: String): String {
        // Avoid replacing a prefix inside a longer path segment. Dump files use separators such as ';', whitespace, and
        // quotes, so the regex requires a path boundary after the matched owner path.
        val pathBoundary = "(?=$|[/;\\s\"'])"
        return Regex("${Regex.escape(from)}$pathBoundary").replace(this, Regex.escapeReplacement(to))
    }

    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
        private const val COPIED_DERIVED_DATA_MARKER = ".swiftpm-xcode-dump-copied-from-owner"
    }
}
