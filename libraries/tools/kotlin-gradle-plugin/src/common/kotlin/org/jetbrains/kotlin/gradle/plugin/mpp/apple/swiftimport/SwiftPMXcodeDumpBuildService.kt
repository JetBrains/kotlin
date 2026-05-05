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
    val packageResolvedHash: String?,
    val identifierDepsHash: String,
)

internal interface SwiftPMXcodeDumpBuildServiceParameters : BuildServiceParameters {
    val latestStateFile: RegularFileProperty
}

internal abstract class SwiftPMXcodeDumpBuildService : BuildService<SwiftPMXcodeDumpBuildServiceParameters> {

    private val stateLock = Any()

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
            loadLatestStateIfNeeded(sdkDerivedDataDirName)

            val existingByPackageResolved = packageResolvedHash?.let { bucketsByPackageResolvedHash[it] }
            if (existingByPackageResolved != null) {
                bucketsByIdentifierDepsHash.putIfAbsent(identifierDepsHash, existingByPackageResolved)
                return XcodeDumpClaim.Existing(existingByPackageResolved)
            }

            val existingByIdentifierDeps = bucketsByIdentifierDepsHash[identifierDepsHash]
            if (existingByIdentifierDeps != null) {
                packageResolvedHash?.let { bucketsByPackageResolvedHash.putIfAbsent(it, existingByIdentifierDeps) }
                return XcodeDumpClaim.Existing(existingByIdentifierDeps)
            }

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
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket.id}'", it)
        }
    }

    fun markXcodeDumpCompleted(bucket: XcodeDumpBucket) {
        synchronized(stateLock) {
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

        val latestState = runCatching {
            dumpTaskFingerprintJson.decodeFromString<XcodeDumpBuildServiceLatestState>(stateFile.readText())
        }.getOrElse {
            stateFile.delete()
            return
        }

        var hasStaleEntries = false
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
            storeLatestState()
        }
    }

    private fun storeLatestState() {
        val stateFile = parameters.latestStateFile.asFile.get()
        stateFile.parentFile.mkdirs()
        stateFile.writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpBuildServiceLatestState(
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
        if (!ownerDumpDir.resolve("clang_args_dump").isDirectory) return null
        if (!ownerDumpDir.resolve("ld_args_dump").isDirectory) return null
        if (!ownerDerivedDataDir.resolve(sdkDerivedDataDirName).exists()) return null
        if (!ownerDumpDir.hasMatchingFingerprintStamp(expectedPackageResolvedHash, expectedIdentifierDepsHash)) return null

        return XcodeDumpBucket(
            id = id,
            packageResolvedHash = packageResolvedHash,
            identifierDepsHash = identifierDepsHash,
            ownerDumpDir = ownerDumpDir,
            ownerDerivedDataDir = ownerDerivedDataDir,
            ownerSyntheticImportProjectRoot = File(ownerSyntheticImportProjectRoot),
            ownerSwiftPMDependenciesCheckout = File(ownerSwiftPMDependenciesCheckout),
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

        val stamp = runCatching {
            dumpTaskFingerprintJson.decodeFromString<XcodeDumpOutputFingerprintStamp>(stampFile.readText())
        }.getOrNull() ?: return false

        return when {
            expectedPackageResolvedHash != null -> stamp.packageResolvedHash == expectedPackageResolvedHash
            expectedIdentifierDepsHash != null -> stamp.identifierDepsHash == expectedIdentifierDepsHash
            else -> false
        }
    }

    companion object {
        private const val SERVICE_NAME = "swiftPMXcodeDumpBuildService"
        private const val FINGERPRINT_STAMP_FILE_NAME = "xcode-dump-fingerprint.json"

        /** Registers the shared service once per build. */
        fun registerIfAbsent(project: Project): Provider<SwiftPMXcodeDumpBuildService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                SwiftPMXcodeDumpBuildService::class.java
            ) {
                it.parameters.latestStateFile.fileValue(
                    project.rootDir.resolve(".gradle/kotlin/swiftPMXcodeDumpBuildService/latest-state.json")
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
