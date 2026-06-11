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
    private val fetchBucketsBySyntheticPackageFingerprint = mutableMapOf<String, SwiftResolveBucket>()
    private val generatePackageBucketBySyntheticPackageFingerprint = mutableMapOf<String, GeneratePackageBucket>()


    fun claimOrJoinPackageGeneration(
        packageHash: String,
    ): CoordinationClaim<GeneratePackageBucket> {
        synchronized(stateLock) {
            val existingByPackageHash = generatePackageBucketBySyntheticPackageFingerprint[packageHash]
            if (existingByPackageHash != null) return CoordinationClaim.Existing(existingByPackageHash)

            val reusableBucket = findReusablePackageGenerationInSharedRoot(
                packageHash
            )

            if (reusableBucket != null) {
                generatePackageBucketBySyntheticPackageFingerprint[packageHash] = reusableBucket
                return CoordinationClaim.Existing(reusableBucket)
            }

            val newBucket = GeneratePackageBucket(
                ownerSyntheticPackageRoot = sharedPackageGenerationRoot(packageHash)
            )

            generatePackageBucketBySyntheticPackageFingerprint[packageHash] = newBucket
            return CoordinationClaim.Owner(newBucket)
        }
    }

    fun claimOrJoinSwiftResolve(
        packageHash: String,
    ): CoordinationClaim<SwiftResolveBucket> {
        synchronized(stateLock) {
            val existingByExecutionHash = fetchBucketsBySyntheticPackageFingerprint[packageHash]
            if (existingByExecutionHash != null) return CoordinationClaim.Existing(existingByExecutionHash)


            val reusableBucket = findReusableFetchBucketInSharedRoot(
                packageHash
            )
            if (reusableBucket != null) {
                fetchBucketsBySyntheticPackageFingerprint[packageHash] = reusableBucket
                return CoordinationClaim.Existing(reusableBucket)
            }

            val packageRoot = sharedPackageGenerationRoot(packageHash)
            val checkoutDir = sharedCheckoutDir(packageHash)
            val newBucket = SwiftResolveBucket(
                ownerPackageResolvedFile = sharedPackageResolved(packageRoot),
                ownerWorkspaceStateFile = sharedCheckoutWorkspaceStateJsonFile(checkoutDir),
                ownerSwiftPMDependenciesCheckout = checkoutDir,
                ownerSyntheticImportProjectRoot = packageRoot,
            )

            fetchBucketsBySyntheticPackageFingerprint[packageHash] = newBucket
            return CoordinationClaim.Owner(newBucket)
        }
    }

    fun claimOrJoinXcodeDump(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
    ): CoordinationClaim<XcodeDumpBucket> {
        synchronized(stateLock) {
            val executionKey = XcodeDumpBucketMapKey(xcodebuildExecutionHash, xcodebuildSdk)
            val existingByExecutionHash = dumpBucketsByXcodebuildFingerprint[executionKey]
            if (existingByExecutionHash != null) return CoordinationClaim.Existing(existingByExecutionHash)

            val reusableBucket = findReusableDumpBucketInSharedRoot(
                xcodebuildExecutionHash = xcodebuildExecutionHash,
                xcodebuildSdk = xcodebuildSdk,
                sdkDerivedDataDirName = "dd_$xcodebuildSdk",
            )
            if (reusableBucket != null) {
                dumpBucketsByXcodebuildFingerprint[executionKey] = reusableBucket
                return CoordinationClaim.Existing(reusableBucket)
            }

            // No reusable owner exists. The current task becomes the owner and will run xcodebuild into the root-build
            // bucket. The owner still builds its own synthetic package and uses its own SwiftPM checkout; only the dump
            // and DerivedData output locations are shared.
            val bucketId = xcodebuildExecutionHash
            val bucketRoot = sharedDumpBucketRoot(bucketId)
            val newBucket = XcodeDumpBucket(
                ownerDumpDir = sharedDumpDir(bucketRoot, xcodebuildSdk),
                ownerDerivedDataDir = sharedDerivedDataDir(bucketRoot),
                ownerMarkerFile = sharedDumpBucketMarker(bucketRoot, xcodebuildSdk)
            )
            dumpBucketsByXcodebuildFingerprint[executionKey] = newBucket
            return CoordinationClaim.Owner(newBucket)
        }
    }

    private fun sharedDumpBucketRoot(bucketId: String): File =
        parameters.sharedXcodeDumpRoot.get().asFile.resolve(bucketId)

    private fun sharedDumpBucketMarker(bucketRoot: File, xcodebuildSdk: String): File =
        bucketRoot.resolve("$xcodebuildSdk.marker")

    private fun sharedDumpDir(bucketRoot: File, xcodebuildSdk: String): File =
        bucketRoot.resolve("swiftImportClangDump/$xcodebuildSdk")

    private fun sharedDerivedDataDir(bucketRoot: File): File =
        bucketRoot.resolve("swiftImportDd")

    private fun sharedPackageGenerationRoot(packageHash: String): File =
        parameters.sharedSyntheticPackageRoot.get().asFile.resolve(packageHash)

    private fun sharedCheckoutDir(packageHash: String): File =
        parameters.sharedCheckoutDirectoryRoot.get().asFile.resolve(packageHash)

    private fun sharedCheckoutWorkspaceStateJsonFile(checkoutDir: File): File =
        checkoutDir.resolve("workspace-state.json")

    private fun sharedPackageResolved(packageDir: File): File =
        packageDir.resolve("Package.resolved")

    fun awaitPackageGeneration(bucket : GeneratePackageBucket){
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM package generation failed for bucket '${bucket}'", it)

        }
    }

    fun markPackageGenerationCompleted(bucket : GeneratePackageBucket) {
        synchronized(stateLock){
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

    fun awaitXcodeDump(bucket: XcodeDumpBucket) {
        // Joined tasks wait here instead of depending on an owner task. At execution time the Gradle task graph is already
        // fixed, so a latch inside the build service is the safe coordination primitive.
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket}'", it)
        }
    }

    fun markXcodeDumpCompleted(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
    ) {
        synchronized(stateLock) {
            val key = XcodeDumpBucketMapKey(xcodebuildExecutionHash, xcodebuildSdk)
            val bucket = dumpBucketsByXcodebuildFingerprint[key]
                ?: error("Xcode dump bucket is missing for $key")
            bucket.completed = true
            bucket.completion.countDown()
        }

    }


    fun markXcodeDumpCompleted(bucket: XcodeDumpBucket) {
        synchronized(stateLock) {
            // The stamp protects root-build directory reuse: existing files alone are not enough because a later dump
            // can overwrite the same deterministic bucket directory with a different fingerprint.
            bucket.completed = true
            bucket.completion.countDown()
        }
    }

    fun markXcodeDumpFailed(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
        failure: Throwable,
    ) {
        synchronized(stateLock) {
            val key = XcodeDumpBucketMapKey(xcodebuildExecutionHash, xcodebuildSdk)
            val bucket = dumpBucketsByXcodebuildFingerprint[key]
                ?: error("Xcode dump bucket is missing for $key")
            bucket.failure = failure
            bucket.completion.countDown()
        }

    }

    fun markXcodeDumpFailed(bucket: XcodeDumpBucket, failure: Throwable) {
        synchronized(stateLock) {
            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    fun markSwiftResolveCompleted(packageHash: String) {
        synchronized(stateLock) {
            val bucket = fetchBucketsBySyntheticPackageFingerprint[packageHash]
                ?: error("Swift resolve bucket is missing for package hash $packageHash")

            bucket.completed = true
            bucket.completion.countDown()
        }
    }

    fun markSwiftResolveFailed(packageHash: String, failure: Throwable) {
        synchronized(stateLock) {
            val bucket = fetchBucketsBySyntheticPackageFingerprint[packageHash]
                ?: error("Swift resolve bucket is missing for package hash $packageHash")

            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    fun markSwiftResolveCompleted(bucket: SwiftResolveBucket) {
        synchronized(stateLock) {
            bucket.completed = true
            bucket.completion.countDown()
        }
    }

    fun markSwiftResolveFailed(bucket: SwiftResolveBucket, failure: Throwable) {
        synchronized(stateLock) {
            bucket.failure = failure
            bucket.completion.countDown()
        }
    }

    fun awaitSwiftResolved(bucket: SwiftResolveBucket) {
        // Joined tasks wait here instead of depending on an owner task. At execution time the Gradle task graph is already
        // fixed, so a latch inside the build service is the safe coordination primitive.
        bucket.completion.await()
        bucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket}'", it)
        }
    }

    fun findSwiftResolveBucket(packageHash: String): SwiftResolveBucket? =
        synchronized(stateLock) {
            fetchBucketsBySyntheticPackageFingerprint[packageHash]
                ?: findReusableFetchBucketInSharedRoot(packageHash)?.also {
                    fetchBucketsBySyntheticPackageFingerprint[packageHash] = it
                }
        }

    fun findPackageGenerationBucket(packageHash: String): GeneratePackageBucket? =
        synchronized(stateLock) {
            generatePackageBucketBySyntheticPackageFingerprint[packageHash]
                ?: findReusablePackageGenerationInSharedRoot(packageHash)?.also {
                    generatePackageBucketBySyntheticPackageFingerprint[packageHash] = it
                }
        }

    private fun <T : CoordinationBucket> completedBucket(
        bucket: (CountDownLatch) -> T,
    ): T = bucket(CountDownLatch(0)).also {
        it.completed = true
    }

    /**
     * For all of reusable bucket finding strategies below, the idea is pretty greedy for now.
     *
     * Alternatively, we could also dump hash the whole bucket contents and save it alongside with bucket in file system,
     * and then compare the hash to decide whether we can reuse the bucket or not.
     */

    private fun findReusablePackageGenerationInSharedRoot(
        packageHash: String,
    ): GeneratePackageBucket? {
        val syntheticPackageRoot = sharedPackageGenerationRoot(packageHash)
        val expectedPackageManifest = syntheticPackageRoot.resolve("Package.swift")

        if (!expectedPackageManifest.exists()) return null

        return completedBucket { completion ->
            GeneratePackageBucket(
                ownerSyntheticPackageRoot = syntheticPackageRoot,
                completion = completion,
            )
        }
    }

    private fun findReusableFetchBucketInSharedRoot(
        packageHash: String,
    ): SwiftResolveBucket? {
        val syntheticPackageRoot = sharedPackageGenerationRoot(packageHash)
        val checkoutDir = sharedCheckoutDir(packageHash)
        val packageResolved = sharedPackageResolved(syntheticPackageRoot)
        val workspaceState = sharedCheckoutWorkspaceStateJsonFile(checkoutDir)

        if (!workspaceState.exists()) return null

        return completedBucket { completion ->
            SwiftResolveBucket(
                ownerPackageResolvedFile = packageResolved,
                ownerWorkspaceStateFile = workspaceState,
                ownerSwiftPMDependenciesCheckout = checkoutDir,
                ownerSyntheticImportProjectRoot = syntheticPackageRoot,
                completion = completion,
            )
        }
    }

    private fun findReusableDumpBucketInSharedRoot(
        xcodebuildExecutionHash: String,
        xcodebuildSdk: String,
        sdkDerivedDataDirName: String,
    ): XcodeDumpBucket? {
        val bucketRoot = sharedDumpBucketRoot(xcodebuildExecutionHash)
        val ownerDumpDir = sharedDumpDir(bucketRoot, xcodebuildSdk)
        val ownerDerivedDataDir = sharedDerivedDataDir(bucketRoot)
        val ownerMarkerFile = sharedDumpBucketMarker(bucketRoot, xcodebuildSdk)

        // A root-build bucket is reusable only if both logical outputs still exist: dumped clang/ld args and the SDK
        // DerivedData directory that contains the products referenced by those args.
        if (!ownerDumpDir.resolve("clang_args_dump").isDirectory) return null
        if (!ownerDumpDir.resolve("ld_args_dump").isDirectory) return null
        if (!ownerDerivedDataDir.resolve(sdkDerivedDataDirName).exists()) return null
        if (!ownerMarkerFile.exists()) return null

        return completedBucket { completion ->
            XcodeDumpBucket(
                ownerDumpDir = ownerDumpDir,
                ownerDerivedDataDir = ownerDerivedDataDir,
                completion = completion,
                ownerMarkerFile = ownerMarkerFile
            )
        }
    }


    companion object {
        private const val SERVICE_NAME = "SwiftImportFingerprintedCoordinationService"

        /**
         * Registers the shared service once per build.
         */
        fun registerIfAbsent(
            project: Project,
            xcodeDumpsDir: Provider<Directory>,
            checkoutDir: Provider<Directory>,
            generatePackageDir: Provider<Directory>,
        ): Provider<SwiftImportFingerprintedCoordinationService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                SwiftImportFingerprintedCoordinationService::class.java
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

private data class XcodeDumpBucketMapKey(
    val xcodebuildFingerprint: String,
    val xcodebuildSdk: String,
)



internal open class CoordinationBucket(
    val completion: CountDownLatch = CountDownLatch(1),
    var failure: Throwable? = null,
    var completed: Boolean = false,
)

internal class XcodeDumpBucket(
    val ownerDumpDir: File,
    val ownerDerivedDataDir: File,
    val ownerMarkerFile: File,
    completion: CountDownLatch = CountDownLatch(1),
) : CoordinationBucket(completion)

internal class SwiftResolveBucket(
    // these first two are already markers for swift package resolve
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
internal sealed class CoordinationClaim<out T : CoordinationBucket> {
    abstract val bucket: T

    data class Owner<T : CoordinationBucket>(
        override val bucket: T,
    ) : CoordinationClaim<T>()

    data class Existing<T : CoordinationBucket>(
        override val bucket: T,
    ) : CoordinationClaim<T>()
}
