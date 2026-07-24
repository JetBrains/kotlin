/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.io.File
import java.util.concurrent.CountDownLatch

internal interface SwiftImportFingerprintedCoordinationServiceParameters : BuildServiceParameters {
    val sharedXcodeDumpRoot: DirectoryProperty
    val sharedSyntheticPackageRoot: DirectoryProperty
    val sharedCheckoutDirectoryRoot: DirectoryProperty
}

internal abstract class SwiftImportFingerprintedCoordinationService : BuildService<SwiftImportFingerprintedCoordinationServiceParameters> {

    /**
     * All mutable service state is guarded by this lock because multiple dump tasks can execute in parallel.
     *
     * The task graph cannot be changed at execution time, so coordination happens by sharing an execution bucket:
     * one task becomes the owner, other matching tasks wait for the owner and then point downstream work at the same
     * shared outputs.
     */
    private val stateLock = Any()

    /** In-memory buckets for the current Gradle invocation, keyed by the xcodebuild execution fingerprint. */
    private val dumpBucketsByXcodebuildFingerprint = mutableMapOf<XcodeDumpBucketMapKey, XcodeDumpBucket>()
    private val fetchBucketsBySyntheticPackageFingerprint = mutableMapOf<SwiftResolveBucketMapKey, SwiftResolveBucket>()
    private val generatePackageBucketBySyntheticPackageFingerprint = mutableMapOf<GeneratePackageBucketMapKey, GeneratePackageBucket>()

    private inline fun <K, B : CoordinationBucket> claimOrJoin(
        key: K,
        buckets: MutableMap<K, B>,
        createOwner: (K) -> B,
    ): CoordinationClaim<B> {
        synchronized(stateLock) {
            buckets[key]?.let { return CoordinationClaim.Existing(it) }
            val newBucket = createOwner(key)
            buckets[key] = newBucket
            return CoordinationClaim.Owner(newBucket)
        }
    }

    fun claimOrJoinPackageGeneration(
        packageHash: String,
    ): CoordinationClaim<GeneratePackageBucket> =
        claimOrJoin(
            key = GeneratePackageBucketMapKey(packageHash),
            buckets = generatePackageBucketBySyntheticPackageFingerprint,
            createOwner = {
                GeneratePackageBucket(
                    ownerSyntheticPackageRoot = sharedPackageGenerationRoot(packageHash)
                )
            }
        )

    fun claimOrJoinSwiftResolve(
        packageHash: String,
    ): CoordinationClaim<SwiftResolveBucket> =
        claimOrJoin(
            key = SwiftResolveBucketMapKey(packageHash),
            buckets = fetchBucketsBySyntheticPackageFingerprint,
            createOwner = { key ->
                val packageRoot = sharedPackageGenerationRoot(packageHash)

                val checkoutDir = sharedCheckoutDir(packageHash)

                SwiftResolveBucket(
                    key = key,
                    ownerPackageResolvedFile = sharedPackageResolved(packageRoot),
                    ownerWorkspaceStateFile = sharedCheckoutWorkspaceStateJsonFile(checkoutDir),
                    ownerSwiftPMDependenciesCheckout = checkoutDir,
                    ownerSyntheticImportProjectRoot = packageRoot,
                )
            }
        )

    fun claimOrJoinXcodeDump(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
    ): CoordinationClaim<XcodeDumpBucket> =
        claimOrJoin(
            key = XcodeDumpBucketMapKey(xcodebuildExecutionHash, xcodebuildSdk),
            buckets = dumpBucketsByXcodebuildFingerprint,
            createOwner = { key ->
                XcodeDumpBucket(
                    key = key,
                    ownerDumpDir = sharedXcodeDumpDir(xcodebuildExecutionHash, xcodebuildSdk),
                    ownerDerivedDataDir = sharedXcodeDerivedDataDir(xcodebuildExecutionHash, xcodebuildSdk),
                )
            }
        )

    fun sharedXcodeDumpDir(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
    ) = sharedDumpDir(sharedDumpBucketRoot(xcodebuildExecutionHash), xcodebuildSdk)

    fun sharedXcodeDerivedDataDir(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
    ) = sharedDerivedDataDir(sharedDumpBucketRoot(xcodebuildExecutionHash), xcodebuildSdk)

    private fun sharedDumpBucketRoot(bucketId: String): File =
        parameters.sharedXcodeDumpRoot.get().asFile.resolve(bucketId)

    private fun sharedDumpDir(bucketRoot: File, xcodebuildSdk: String): File =
        bucketRoot.resolve("swiftImportClangDump/$xcodebuildSdk")

    private fun sharedDerivedDataDir(bucketRoot: File, xcodebuildSdk: String): File =
        bucketRoot.resolve("swiftImportDd").resolve("dd_$xcodebuildSdk")

    internal fun sharedPackageGenerationRoot(packageHash: String): File =
        parameters.sharedSyntheticPackageRoot.get().asFile.resolve(packageHash)

    internal fun sharedCheckoutDir(packageHash: String): File =
        parameters.sharedCheckoutDirectoryRoot.get().asFile.resolve(packageHash)

    private fun sharedCheckoutWorkspaceStateJsonFile(checkoutDir: File): File =
        checkoutDir.resolve("workspace-state.json")

    private fun sharedPackageResolved(packageDir: File): File =
        packageDir.resolve("Package.resolved")

    fun awaitPackageGeneration(bucket: GeneratePackageBucket) {
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM package generation failed for bucket '${bucket}'", it)

        }
    }

    fun markPackageGenerationCompleted(bucket: GeneratePackageBucket) {
        synchronized(stateLock) {
            bucket.completed = true
            bucket.completion.countDown()
        }
    }

    fun markPackageGenerationFailed(bucket: GeneratePackageBucket, failure: Throwable) {
        synchronized(stateLock) {
            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    fun awaitXcodeDump(key: XcodeDumpBucketMapKey) {
        val bucket = synchronized(stateLock) {
            dumpBucketsByXcodebuildFingerprint[key]
        } ?: error("No bucket found for key $key")
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket}'", it)
        }
    }

    fun awaitXcodeDumpOwnerStarted(key: XcodeDumpBucketMapKey) {
        val bucket = synchronized(stateLock) {
            dumpBucketsByXcodebuildFingerprint[key]
        } ?: error("No bucket found for key $key")

        // Do not let a joiner submit its blocking worker until the owner worker has actually started.
        // Otherwise, under a saturated Worker API pool, joiners can occupy the last slots while waiting
        // and prevent the owner from ever starting.
        bucket.ownerStarted.await()
    }

    fun markXcodeDumpStarted(key: XcodeDumpBucketMapKey) {
        synchronized(stateLock) {
            val bucket = dumpBucketsByXcodebuildFingerprint[key]
                ?: error("Xcode dump bucket is missing for $key")

            bucket.ownerStarted.countDown()
        }
    }

    fun markXcodeDumpCompleted(
        key: XcodeDumpBucketMapKey,
    ) {
        synchronized(stateLock) {
            val bucket = dumpBucketsByXcodebuildFingerprint[key]
                ?: error("Xcode dump bucket is missing for $key")
            bucket.completed = true
            bucket.completion.countDown()
        }

    }

    fun markXcodeDumpFailed(
        key: XcodeDumpBucketMapKey,
        failure: Throwable,
    ) {
        synchronized(stateLock) {
            val bucket = dumpBucketsByXcodebuildFingerprint[key]
                ?: error("Xcode dump bucket is missing for $key")
            bucket.failure = failure
            bucket.completion.countDown()
        }

    }

    fun markSwiftResolveCompleted(packageHash: SwiftResolveBucketMapKey) {
        synchronized(stateLock) {
            val bucket = fetchBucketsBySyntheticPackageFingerprint[packageHash]
                ?: error("Swift resolve bucket is missing for package hash $packageHash")

            bucket.completed = true
            bucket.completion.countDown()
        }
    }

    fun markSwiftResolveFailed(packageHash: SwiftResolveBucketMapKey, failure: Throwable) {
        synchronized(stateLock) {
            val bucket = fetchBucketsBySyntheticPackageFingerprint[packageHash]
                ?: error("Swift resolve bucket is missing for package hash $packageHash")

            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    fun awaitSwiftResolved(key: SwiftResolveBucketMapKey) {
        // Joined tasks wait here instead of depending on an owner task. At execution time the Gradle task graph is already
        // fixed, so a latch inside the build service is the safe coordination primitive.
        val bucket = synchronized(stateLock) {
            fetchBucketsBySyntheticPackageFingerprint[key]
        } ?: error("Swift resolve bucket is missing for package hash $key")

        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket}'", it)
        }
    }

    fun awaitSwiftResolveOwnerStarted(key: SwiftResolveBucketMapKey) {
        val bucket = synchronized(stateLock) {
            fetchBucketsBySyntheticPackageFingerprint[key]
        } ?: error("Swift resolve bucket is missing for package hash $key")

        // Do not let a joiner submit its blocking worker until the owner worker has actually started.
        // Otherwise, under a saturated Worker API pool, joiners can occupy the last slots while waiting
        // and prevent the owner from ever starting.
        bucket.ownerStarted.await()
    }

    fun markSwiftResolveStarted(packageHash: SwiftResolveBucketMapKey) {
        synchronized(stateLock) {
            val bucket = fetchBucketsBySyntheticPackageFingerprint[packageHash]
                ?: error("Swift resolve bucket is missing for package hash $packageHash")

            bucket.ownerStarted.countDown()
        }
    }

    companion object {
        /**
         * Registers the shared service once per build.
         */
        fun registerIfAbsent(
            project: Project,
            xcodeDumpsDir: Provider<Directory>,
            checkoutDir: Provider<Directory>,
            generatePackageDir: Provider<Directory>,
        ): Provider<SwiftImportFingerprintedCoordinationService> =
            project.gradle.registerClassLoaderScopedBuildService(
                SwiftImportFingerprintedCoordinationService::class
            ) { buildServiceSpec ->
                buildServiceSpec.parameters.sharedXcodeDumpRoot.set(
                    xcodeDumpsDir
                )
                buildServiceSpec.parameters.sharedSyntheticPackageRoot.set(
                    generatePackageDir
                )
                buildServiceSpec.parameters.sharedCheckoutDirectoryRoot.set(
                    checkoutDir
                )
            }
    }
}

internal data class XcodeDumpBucketMapKey(
    val xcodebuildFingerprint: String,
    val xcodebuildSdk: String,
) : java.io.Serializable

internal data class SwiftResolveBucketMapKey(
    val value: String
) : java.io.Serializable

internal data class GeneratePackageBucketMapKey(
    val value: String
) : java.io.Serializable

internal open class CoordinationBucket(
    val completion: CountDownLatch = CountDownLatch(1),
    val ownerStarted: CountDownLatch = CountDownLatch(1),
    var failure: Throwable? = null,
    var completed: Boolean = false,
)

internal class XcodeDumpBucket(
    val key: XcodeDumpBucketMapKey,
    val ownerDumpDir: File,
    val ownerDerivedDataDir: File,
    completion: CountDownLatch = CountDownLatch(1),
) : CoordinationBucket(completion)

internal class SwiftResolveBucket(
    // these first two are already markers for swift package resolve
    val key: SwiftResolveBucketMapKey,
    val ownerPackageResolvedFile: File,
    val ownerWorkspaceStateFile: File,
    val ownerSwiftPMDependenciesCheckout: File,
    val ownerSyntheticImportProjectRoot: File,
    completion: CountDownLatch = CountDownLatch(1),
) : CoordinationBucket(completion)

internal class GeneratePackageBucket(
    val ownerSyntheticPackageRoot: File,
    completion: CountDownLatch = CountDownLatch(1),
) : CoordinationBucket(completion)

/**
 * Result of trying to acquire a bucket.
 *
 * [Owner] means the caller must run xcodebuild and then mark the bucket as completed/failed.
 * [Existing] means another task or a previous invocation already owns reusable outputs, so the caller waits and
 * writes its local location marker to those shared outputs.
 */
internal sealed class CoordinationClaim<out T : CoordinationBucket> : java.io.Serializable {
    abstract val bucket: T

    data class Owner<T : CoordinationBucket>(
        override val bucket: T,
    ) : CoordinationClaim<T>()

    data class Existing<T : CoordinationBucket>(
        override val bucket: T,
    ) : CoordinationClaim<T>()
}
