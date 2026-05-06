/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.util.concurrent.CountDownLatch

@Serializable
internal data class XcodeDumpSharingFingerprints(
    /**
     * Preferred sharing key. It is based on the generated Package.resolved and other dump-relevant inputs.
     *
     * This value is nullable because Package.resolved is produced by another task and may be absent in scenarios
     * where SwiftPM resolution has not produced a lock file yet. In that case, we fall back to [identifierDepsHash].
     */
    val packageResolvedHash: String?,

    /**
     * Fallback sharing key. It is based on the Package.resolved synchronization identifier, direct/transitive SwiftPM
     * dependencies, and build settings that can affect the generated xcodebuild command.
     */
    val identifierDepsHash: String,
)

internal interface SwiftPMXcodeDumpBuildServiceParameters : BuildServiceParameters {
    val latestStateFile: RegularFileProperty
}

internal abstract class SwiftPMXcodeDumpBuildService : BuildService<SwiftPMXcodeDumpBuildServiceParameters> {

    /**
     * All mutable service state is guarded by this lock because multiple dump tasks can execute in parallel.
     *
     * The task graph cannot be changed at execution time, so coordination happens by sharing an execution bucket:
     * one task becomes the owner, other matching tasks wait for the owner and copy/remap its outputs locally.
     */
    private val stateLock = Any()

    /**
     * In-memory buckets for the current Gradle invocation.
     *
     * Both maps may point to the same bucket. We prefer Package.resolved matching when possible, but keep the fallback
     * identifier/dependencies mapping as an alias so later tasks can join by either key.
     */
    private val bucketsByPackageResolvedHash = mutableMapOf<String, XcodeDumpBucket>()
    private val bucketsByIdentifierDepsHash = mutableMapOf<String, XcodeDumpBucket>()
    private var latestStateLoaded = false

    class XcodeDumpBucket(
        val id: String,
        val packageResolvedHash: String?,
        val identifierDepsHash: String,
        val ownerDumpDir: File,
        val ownerDerivedDataDir: File,
        val ownerSyntheticImportProjectRoot: File,
        val ownerSwiftPMDependenciesCheckout: File,
        val completion: CountDownLatch = CountDownLatch(1),
        var failure: Throwable? = null,
        var completed: Boolean = false,
    )

    /**
     * Result of trying to acquire a bucket.
     *
     * [Owner] means the caller must run xcodebuild and then mark the bucket as completed/failed.
     * [Existing] means another task or a previous invocation already owns reusable outputs, so the caller waits and
     * materializes those outputs into its own local task output directories.
     */
    sealed class XcodeDumpClaim {
        abstract val bucket: XcodeDumpBucket

        data class Owner(override val bucket: XcodeDumpBucket) : XcodeDumpClaim()
        data class Existing(override val bucket: XcodeDumpBucket) : XcodeDumpClaim()
    }

    fun claimOrJoinXcodeDump(
        packageResolvedHash: String?,
        identifierDepsHash: String,
        sdkDerivedDataDirName: String,
        ownerDumpDir: File,
        ownerDerivedDataDir: File,
        syntheticImportProjectRoot: File,
        swiftPMDependenciesCheckout: File,
    ): XcodeDumpClaim {
        synchronized(stateLock) {
            // Loading is lazy because not every build executes SwiftPM dump tasks. Validation depends on the SDK-specific
            // DerivedData directory name, so it is done from the first claim rather than service construction.
            loadLatestStateIfNeeded(sdkDerivedDataDirName)

            // Exact Package.resolved match wins over the fallback. This catches projects that use different lock
            // identifiers but resolve to the same SwiftPM graph.
            val existingByPackageResolved = packageResolvedHash?.let { bucketsByPackageResolvedHash[it] }
            if (existingByPackageResolved != null) {
                // Add the fallback alias for future tasks in this invocation. This does not change ownership.
                bucketsByIdentifierDepsHash.putIfAbsent(identifierDepsHash, existingByPackageResolved)
                return XcodeDumpClaim.Existing(existingByPackageResolved)
            }

            // Fallback for cases where Package.resolved is unavailable or not equal but the declared graph/build
            // settings are equivalent enough to share the xcodebuild dump.
            val existingByIdentifierDeps = bucketsByIdentifierDepsHash[identifierDepsHash]
            if (existingByIdentifierDeps != null) {
                // If this task has a Package.resolved key, remember it as another alias to the same bucket.
                packageResolvedHash?.let { bucketsByPackageResolvedHash.putIfAbsent(it, existingByIdentifierDeps) }
                return XcodeDumpClaim.Existing(existingByIdentifierDeps)
            }

            // No reusable owner exists. The current task becomes the owner and will run xcodebuild into its local dirs.
            val bucketId = packageResolvedHash ?: identifierDepsHash
            val newBucket = XcodeDumpBucket(
                id = bucketId,
                packageResolvedHash = packageResolvedHash,
                identifierDepsHash = identifierDepsHash,
                ownerDumpDir = ownerDumpDir,
                ownerDerivedDataDir = ownerDerivedDataDir,
                ownerSyntheticImportProjectRoot = syntheticImportProjectRoot,
                ownerSwiftPMDependenciesCheckout = swiftPMDependenciesCheckout,
            )
            packageResolvedHash?.let { bucketsByPackageResolvedHash[it] = newBucket }
            bucketsByIdentifierDepsHash[identifierDepsHash] = newBucket
            return XcodeDumpClaim.Owner(newBucket)
        }
    }

    fun awaitXcodeDump(bucket: XcodeDumpBucket) {
        // Losers wait here instead of depending on an owner task. At execution time the Gradle task graph is already
        // fixed, so a latch inside the build service is the safe coordination primitive.
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket.id}'", it)
        }
    }

    fun markXcodeDumpCompleted(bucket: XcodeDumpBucket) {
        synchronized(stateLock) {
            // The stamp protects persisted state from reusing an owner directory after it was overwritten by a later
            // dump with a different fingerprint.
            bucket.writeFingerprintStamp()
            bucket.completed = true
            storeLatestState()
            bucket.completion.countDown()
        }
    }

    fun markXcodeDumpFailed(bucket: XcodeDumpBucket, failure: Throwable) {
        synchronized(stateLock) {
            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    private fun loadLatestStateIfNeeded(sdkDerivedDataDirName: String) {
        if (latestStateLoaded) return
        latestStateLoaded = true

        val stateFile = parameters.latestStateFile.asFile.get()
        if (!stateFile.exists()) return

        // The state file is an optimization only. If it is corrupt, delete it and let the current build recreate state
        // from tasks that actually run.
        val latestState = runCatching {
            dumpTaskFingerprintJson.decodeFromString<XcodeDumpBuildServiceLatestState>(stateFile.readText())
        }.getOrElse {
            stateFile.delete()
            return
        }

        var hasStaleEntries = false
        // Restore entries keyed by Package.resolved first. Each entry is validated against the current filesystem
        // because clean builds, deleted owner projects, or overwritten owner directories must not be reused.
        latestState.bucketsByPackageResolvedHash.forEach { (fingerprint, cacheEntry) ->
            val reusableBucket = cacheEntry.toReusableBucket(
                expectedPackageResolvedHash = fingerprint,
                expectedIdentifierDepsHash = null,
                sdkDerivedDataDirName = sdkDerivedDataDirName,
            )
            if (reusableBucket != null) {
                bucketsByPackageResolvedHash[fingerprint] = reusableBucket
            } else {
                hasStaleEntries = true
            }
        }
        // Restore fallback-keyed entries independently. A single physical bucket can be restored through both maps.
        latestState.bucketsByIdentifierDepsHash.forEach { (fingerprint, cacheEntry) ->
            val reusableBucket = cacheEntry.toReusableBucket(
                expectedPackageResolvedHash = null,
                expectedIdentifierDepsHash = fingerprint,
                sdkDerivedDataDirName = sdkDerivedDataDirName,
            )
            if (reusableBucket != null) {
                bucketsByIdentifierDepsHash[fingerprint] = reusableBucket
            } else {
                hasStaleEntries = true
            }
        }

        if (hasStaleEntries) {
            // Prune invalid entries so later Gradle invocations do not repeatedly parse stale paths.
            storeLatestState()
        }
    }

    private fun storeLatestState() {
        val stateFile = parameters.latestStateFile.asFile.get()
        stateFile.parentFile.mkdirs()
        stateFile.writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpBuildServiceLatestState(
                    // Persist only successful completed buckets. In-flight and failed owners are useful only inside the
                    // current Gradle invocation and must not be reused later.
                    bucketsByPackageResolvedHash = reusableBucketsByPackageResolvedHash().mapValues { (_, bucket) ->
                        bucket.toCacheEntry()
                    },
                    bucketsByIdentifierDepsHash = reusableBucketsByIdentifierDepsHash().mapValues { (_, bucket) ->
                        bucket.toCacheEntry()
                    },
                )
            )
        )
    }

    private fun reusableBucketsByPackageResolvedHash(): Map<String, XcodeDumpBucket> {
        return bucketsByPackageResolvedHash.filterValues { it.completed && it.failure == null }
    }

    private fun reusableBucketsByIdentifierDepsHash(): Map<String, XcodeDumpBucket> {
        return bucketsByIdentifierDepsHash.filterValues { it.completed && it.failure == null }
    }

    private fun XcodeDumpBuildServiceCacheEntry.toReusableBucket(
        expectedPackageResolvedHash: String?,
        expectedIdentifierDepsHash: String?,
        sdkDerivedDataDirName: String,
    ): XcodeDumpBucket? {
        val ownerDumpDir = File(this.ownerDumpDir)
        val ownerDerivedDataDir = File(this.ownerDerivedDataDir)
        // A persisted entry is reusable only if both logical outputs still exist: dumped clang/ld args and the SDK
        // DerivedData directory that contains the products referenced by those args.
        if (!ownerDumpDir.resolve("clang_args_dump").isDirectory) return null
        if (!ownerDumpDir.resolve("ld_args_dump").isDirectory) return null
        if (!ownerDerivedDataDir.resolve(sdkDerivedDataDirName).exists()) return null
        // Existing paths are not enough. The same owner task output directory can be overwritten by a new dump with a
        // different fingerprint, so verify the stamp written at completion time.
        if (!ownerDumpDir.hasMatchingFingerprintStamp(expectedPackageResolvedHash, expectedIdentifierDepsHash)) return null

        return XcodeDumpBucket(
            id = id,
            packageResolvedHash = packageResolvedHash,
            identifierDepsHash = identifierDepsHash,
            ownerDumpDir = ownerDumpDir,
            ownerDerivedDataDir = ownerDerivedDataDir,
            ownerSyntheticImportProjectRoot = File(ownerSyntheticImportProjectRoot),
            ownerSwiftPMDependenciesCheckout = File(ownerSwiftPMDependenciesCheckout),
            // Persisted buckets are already complete. Loser tasks can pass through awaitXcodeDump immediately and then
            // copy/remap from the restored owner directories.
            completion = CountDownLatch(0),
            completed = true,
        )
    }

    private fun XcodeDumpBucket.toCacheEntry(): XcodeDumpBuildServiceCacheEntry {
        return XcodeDumpBuildServiceCacheEntry(
            id = id,
            packageResolvedHash = packageResolvedHash,
            identifierDepsHash = identifierDepsHash,
            ownerDumpDir = ownerDumpDir.path,
            ownerDerivedDataDir = ownerDerivedDataDir.path,
            ownerSyntheticImportProjectRoot = ownerSyntheticImportProjectRoot.path,
            ownerSwiftPMDependenciesCheckout = ownerSwiftPMDependenciesCheckout.path,
        )
    }

    private fun XcodeDumpBucket.writeFingerprintStamp() {
        // Store the fingerprint next to the dump output itself, not only in latest-state.json. This lets us detect when
        // latest-state still points to a directory whose contents have since been replaced.
        ownerDumpDir.resolve(FINGERPRINT_STAMP_FILE_NAME).writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpOutputFingerprintStamp(
                    packageResolvedHash = packageResolvedHash,
                    identifierDepsHash = identifierDepsHash,
                )
            )
        )
    }

    private fun File.hasMatchingFingerprintStamp(
        expectedPackageResolvedHash: String?,
        expectedIdentifierDepsHash: String?,
    ): Boolean {
        val stampFile = resolve(FINGERPRINT_STAMP_FILE_NAME)
        if (!stampFile.isFile) return false

        // Invalid/missing stamps make the entry non-reusable. Running xcodebuild again is safer than reusing a possibly
        // mismatched dump.
        val stamp = runCatching {
            dumpTaskFingerprintJson.decodeFromString<XcodeDumpOutputFingerprintStamp>(stampFile.readText())
        }.getOrNull() ?: return false

        // Validate against the key that was used to restore this entry. Package.resolved entries must match the exact
        // Package.resolved fingerprint; fallback entries must match the identifier/dependencies fingerprint.
        return when {
            expectedPackageResolvedHash != null -> stamp.packageResolvedHash == expectedPackageResolvedHash
            expectedIdentifierDepsHash != null -> stamp.identifierDepsHash == expectedIdentifierDepsHash
            else -> false
        }
    }

    companion object {
        private const val SERVICE_NAME = "swiftPMXcodeDumpBuildService"
        private const val FINGERPRINT_STAMP_FILE_NAME = "xcode-dump-fingerprint.json"

        /**
         * Registers the shared service once per build.
         *
         * The service state file lives under the root build directory so separate subprojects can share dump results
         * across Gradle invocations without reading another project's model during configuration.
         */
        fun registerIfAbsent(project: Project): Provider<SwiftPMXcodeDumpBuildService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                SwiftPMXcodeDumpBuildService::class.java
            ) {
                it.parameters.latestStateFile.fileValue(
                    project.rootDir.resolve(".kotlin/kotlin/swiftPMXcodeDumpBuildService/latest-state.json")
                )
            }
    }
}

@Serializable
private data class XcodeDumpBuildServiceLatestState(
    val bucketsByPackageResolvedHash: Map<String, XcodeDumpBuildServiceCacheEntry>,
    val bucketsByIdentifierDepsHash: Map<String, XcodeDumpBuildServiceCacheEntry>,
)

@Serializable
private data class XcodeDumpBuildServiceCacheEntry(
    val id: String,
    val packageResolvedHash: String?,
    val identifierDepsHash: String,
    val ownerDumpDir: String,
    val ownerDerivedDataDir: String,
    val ownerSyntheticImportProjectRoot: String,
    val ownerSwiftPMDependenciesCheckout: String,
)

@Serializable
private data class XcodeDumpOutputFingerprintStamp(
    val packageResolvedHash: String?,
    val identifierDepsHash: String,
)

internal val dumpTaskFingerprintJson = Json {
    encodeDefaults = true
}
