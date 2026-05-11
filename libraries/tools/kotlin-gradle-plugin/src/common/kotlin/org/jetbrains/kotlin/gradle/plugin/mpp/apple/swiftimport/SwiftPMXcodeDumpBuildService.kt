/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch

internal interface SwiftPMXcodeDumpBuildServiceParameters : BuildServiceParameters {
    val sharedXcodeDumpRoot: DirectoryProperty
}

internal abstract class SwiftPMXcodeDumpBuildService : BuildService<SwiftPMXcodeDumpBuildServiceParameters> {

    /**
     * All mutable service state is guarded by this lock because multiple dump tasks can execute in parallel.
     *
     * The task graph cannot be changed at execution time, so coordination happens by sharing an execution bucket:
     * one task becomes the owner, other matching tasks wait for the owner and then point downstream work at the same
     * shared outputs.
     */
    private val stateLock = Any()

    /** In-memory buckets for the current Gradle invocation, keyed by the xcodebuild execution fingerprint. */
    private val bucketsByExecutionHash = mutableMapOf<XcodeDumpBucketMapKey, XcodeDumpBucket>()

    class XcodeDumpBucket(
        val id: String,
        val xcodebuildExecutionHash: String,
        val ownerDumpDir: File,
        val ownerDerivedDataDir: File,
        val completion: CountDownLatch = CountDownLatch(1),
        var failure: Throwable? = null,
        var completed: Boolean = false,
    )

    /**
     * Result of trying to acquire a bucket.
     *
     * [Owner] means the caller must run xcodebuild and then mark the bucket as completed/failed.
     * [Existing] means another task or a previous invocation already owns reusable outputs, so the caller waits and
     * writes its local location marker to those shared outputs.
     */
    sealed class XcodeDumpClaim {
        abstract val bucket: XcodeDumpBucket

        data class Owner(override val bucket: XcodeDumpBucket) : XcodeDumpClaim()
        data class Existing(override val bucket: XcodeDumpBucket) : XcodeDumpClaim()
    }

    fun claimOrJoinXcodeDump(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
        sdkDerivedDataDirName: String,
    ): XcodeDumpClaim {
        synchronized(stateLock) {
            val executionKey = XcodeDumpBucketMapKey(xcodebuildExecutionHash, xcodebuildSdk)
            val existingByExecutionHash = bucketsByExecutionHash[executionKey]
            if (existingByExecutionHash != null) return XcodeDumpClaim.Existing(existingByExecutionHash)

            val reusableBucket = findReusableBucketInSharedRoot(
                xcodebuildExecutionHash = xcodebuildExecutionHash,
                xcodebuildSdk = xcodebuildSdk,
                sdkDerivedDataDirName = sdkDerivedDataDirName,
            )
            if (reusableBucket != null) {
                bucketsByExecutionHash[executionKey] = reusableBucket
                return XcodeDumpClaim.Existing(reusableBucket)
            }

            // No reusable owner exists. The current task becomes the owner and will run xcodebuild into the root-build
            // bucket. The owner still builds its own synthetic package and uses its own SwiftPM checkout; only the dump
            // and DerivedData output locations are shared.
            val bucketId = xcodebuildExecutionHash
            val bucketRoot = sharedBucketRoot(bucketId)
            val newBucket = XcodeDumpBucket(
                id = bucketId,
                xcodebuildExecutionHash = xcodebuildExecutionHash,
                ownerDumpDir = sharedDumpDir(bucketRoot, xcodebuildSdk),
                ownerDerivedDataDir = sharedDerivedDataDir(bucketRoot),
            )
            bucketsByExecutionHash[executionKey] = newBucket
            return XcodeDumpClaim.Owner(newBucket)
        }
    }

    private fun sharedBucketRoot(bucketId: String): File =
        parameters.sharedXcodeDumpRoot.get().asFile.resolve(bucketId)

    private fun sharedDumpDir(bucketRoot: File, xcodebuildSdk: String): File =
        bucketRoot.resolve("swiftImportClangDump/$xcodebuildSdk")

    private fun sharedDerivedDataDir(bucketRoot: File): File =
        bucketRoot.resolve("swiftImportDd")

    fun awaitXcodeDump(bucket: XcodeDumpBucket) {
        // Joined tasks wait here instead of depending on an owner task. At execution time the Gradle task graph is already
        // fixed, so a latch inside the build service is the safe coordination primitive.
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket.id}'", it)
        }
    }

    fun markXcodeDumpCompleted(bucket: XcodeDumpBucket) {
        synchronized(stateLock) {
            // The stamp protects root-build directory reuse: existing files alone are not enough because a later dump
            // can overwrite the same deterministic bucket directory with a different fingerprint.
            bucket.writeFingerprintStamp()
            bucket.completed = true
            bucket.completion.countDown()
        }
    }

    fun markXcodeDumpFailed(bucket: XcodeDumpBucket, failure: Throwable) {
        synchronized(stateLock) {
            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    private fun findReusableBucketInSharedRoot(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
        sdkDerivedDataDirName: String,
    ): XcodeDumpBucket? {
        val bucketRoot = sharedBucketRoot(xcodebuildExecutionHash)
        val ownerDumpDir = sharedDumpDir(bucketRoot, xcodebuildSdk)
        val ownerDerivedDataDir = sharedDerivedDataDir(bucketRoot)

        // A root-build bucket is reusable only if both logical outputs still exist: dumped clang/ld args and the SDK
        // DerivedData directory that contains the products referenced by those args.
        if (!ownerDumpDir.resolve("clang_args_dump").isDirectory) return null
        if (!ownerDumpDir.resolve("ld_args_dump").isDirectory) return null
        if (!ownerDerivedDataDir.resolve(sdkDerivedDataDirName).exists()) return null
        // Existing paths are not enough. Verify the stamp written at completion time.
        val hasMatchingStamp = ownerDumpDir.hasMatchingFingerprintStamp(xcodebuildExecutionHash)
        if (!hasMatchingStamp) return null

        return XcodeDumpBucket(
            id = xcodebuildExecutionHash,
            xcodebuildExecutionHash = xcodebuildExecutionHash,
            ownerDumpDir = ownerDumpDir,
            ownerDerivedDataDir = ownerDerivedDataDir,
            // Root-build buckets discovered from disk are already complete. Joined tasks can pass through awaitXcodeDump
            // immediately and then write their local location marker from the restored owner directories.
            completion = CountDownLatch(0),
            completed = true,
        )
    }

    private fun XcodeDumpBucket.writeFingerprintStamp() {
        // Store the fingerprint next to the dump output itself. This lets us validate deterministic root-build bucket
        // directories without a separate state file.
        ownerDumpDir.resolve(FINGERPRINT_STAMP_FILE_NAME).writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpOutputFingerprintStamp(
                    xcodebuildExecutionHash = xcodebuildExecutionHash,
                )
            )
        )
    }

    private fun File.hasMatchingFingerprintStamp(expectedXcodebuildExecutionHash: String): Boolean {
        val stampFile = resolve(FINGERPRINT_STAMP_FILE_NAME)
        if (!stampFile.isFile) return false

        // Invalid/missing stamps make the entry non-reusable. Running xcodebuild again is safer than reusing a possibly
        // mismatched dump.
        val stamp = runCatching {
            dumpTaskFingerprintJson.deserializeFrom<XcodeDumpOutputFingerprintStamp>(
                stampFile.inputStream()
            )
        }.getOrNull() ?: return false

        return stamp.xcodebuildExecutionHash == expectedXcodebuildExecutionHash
    }

    companion object {
        private const val SERVICE_NAME = "swiftPMXcodeDumpBuildService"
        private const val FINGERPRINT_STAMP_FILE_NAME = "xcode-dump-fingerprint.json"

        /**
         * Registers the shared service once per build.
         */
        fun registerIfAbsent(project: Project): Provider<SwiftPMXcodeDumpBuildService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                SwiftPMXcodeDumpBuildService::class.java
            ) {
                it.parameters.sharedXcodeDumpRoot.set(
                    project.objects.directoryProperty()
                        .fileValue(project.rootDir.resolve("build/kotlin/swiftPMXcodeDumps"))
                )
            }
    }
}

private data class  XcodeDumpBucketMapKey(
    val xcodebuildExecutionHash: String,
    val xcodebuildSdk: String,
)

@Serializable
private data class XcodeDumpOutputFingerprintStamp(
    val xcodebuildExecutionHash: String,
)

internal val dumpTaskFingerprintJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> Json.deserializeFrom(
    inputStream: InputStream,
): T =
    decodeFromStream<T>(inputStream)

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> Json.serializeInto(
    value: T,
    outputStream: OutputStream,
) {
    encodeToStream(value, outputStream)
}
