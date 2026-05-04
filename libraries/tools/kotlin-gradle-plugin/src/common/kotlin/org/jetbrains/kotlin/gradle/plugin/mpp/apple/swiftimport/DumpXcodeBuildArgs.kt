/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class DumpXcodeBuildArgs : DefaultTask() {
    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val hasSwiftPMDependencies: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val localPackageSources: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map {
            it.asFile.readLines().filter { line -> line.isNotEmpty() }.map { line -> File(line) }
        }

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val dumpedXcodeBuildArgsDir: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val packageResolvedFile: RegularFileProperty

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

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Input
    abstract val packageResolvedSynchronization: Property<String>

    @get:Input
    abstract val directSwiftPMDependencies: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val transitiveSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>

    @get:Internal
    val sharedDumpIntermediatesDir: DirectoryProperty = project.objects.directoryProperty().convention(
        xcodebuildSdk.flatMap {
            project.layout.buildDirectory.dir(XcodebuildDefFileUtils.sharedDumpRelativeDir(it))
        }
    )

    @get:Internal
    val syntheticImportDd: DirectoryProperty = project.objects.directoryProperty().convention(
        xcodebuildSdk.flatMap {
            project.layout.buildDirectory.dir(XcodebuildDefFileUtils.sharedDdRelativeDir(it))
        }
    )

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Inject
    protected abstract val fs: FileSystemOperations

    @get:Internal
    abstract val coordinationService: Property<SwiftPMXcodeDumpBuildService>

    @TaskAction
    fun dumpXcodeBuildArgs() {
        if (hasSwiftPMDependencies.get()) {
            val identifierDepsFingerprint = normalizedXcodeDumpTaskFingerprintByIdentifierDeps(
                packageResolvedSynchronization.get(),
                xcodebuildPlatform.get(),
                xcodebuildSdk.get(),
                architectures.get(),
                additionalXcodeArgs.get(),
                directSwiftPMDependencies.get(),
                transitiveSwiftPMDependencies.get(),
            )
            val packageResolvedFingerprint = packageResolvedFile.orNull
                ?.asFile
                ?.takeIf { it.exists() }
                ?.let(::normalizedXcodeDumpTaskFingerprintByPackageResolvedFile)

            val claim = coordinationService.get().claimOrJoinXcodeDump(
                packageResolvedHash = packageResolvedFingerprint,
                identifierDepsHash = identifierDepsFingerprint,
                sharedDumpRoot = sharedDumpIntermediatesDir.get().asFile,
            )

            when (claim) {
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Owner -> runSharedXcodeDump(claim.bucket)
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Existing -> coordinationService.get().awaitXcodeDump(claim.bucket)
            }

            copySharedDumpToLocalOutput(claim.bucket.sharedDumpDir)
        }
    }

    private fun runSharedXcodeDump(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        try {
            submitXcodebuildArgsDumpWorkAction(
                sharedDumpDir = bucket.sharedDumpDir,
                sharedDerivedDataDir = syntheticImportDd.get().asFile.resolve(bucket.id),
            )
            workerExecutor.await()
            coordinationService.get().markXcodeDumpCompleted(bucket)
        } catch (failure: Throwable) {
            coordinationService.get().markXcodeDumpFailed(bucket, failure)
            throw failure
        }
    }

    private fun submitXcodebuildArgsDumpWorkAction(
        sharedDumpDir: File,
        sharedDerivedDataDir: File,
    ) {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.fileValue(sharedDerivedDataDir)
            params.clangDumpIntermediatesDir.fileValue(sharedDumpDir)
            params.additionalXcodeArgs.set(additionalXcodeArgs)
        }
    }

    private fun copySharedDumpToLocalOutput(sharedDumpDir: File) {
        val localDumpDir = dumpedXcodeBuildArgsDir.get().asFile
        if (localDumpDir.canonicalFile == sharedDumpDir.canonicalFile) return

        fs.delete {
            it.delete(localDumpDir)
        }
        fs.copy {
            it.from(sharedDumpDir)
            it.into(localDumpDir)
        }
    }

    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
    }
}

internal abstract class SwiftPMXcodeDumpBuildService : BuildService<BuildServiceParameters.None> {

    private val stateLock = Any()

    private val bucketsByPackageResolvedHash = mutableMapOf<String, MutableXcodeDumpBucket>()
    private val bucketsByIdentifierDepsHash = mutableMapOf<String, MutableXcodeDumpBucket>()

    data class XcodeDumpBucket(
        val id: String,
        val sharedDumpDir: File,
    )

    sealed class XcodeDumpClaim {
        abstract val bucket: XcodeDumpBucket

        data class Owner(override val bucket: XcodeDumpBucket) : XcodeDumpClaim()
        data class Existing(override val bucket: XcodeDumpBucket) : XcodeDumpClaim()
    }

    fun claimOrJoinXcodeDump(
        packageResolvedHash: String?,
        identifierDepsHash: String,
        sharedDumpRoot: File,
    ): XcodeDumpClaim {
        synchronized(stateLock) {
            val existingByPackageResolved = packageResolvedHash?.let { bucketsByPackageResolvedHash[it] }
            if (existingByPackageResolved != null) {
                bucketsByIdentifierDepsHash.putIfAbsent(identifierDepsHash, existingByPackageResolved)
                return XcodeDumpClaim.Existing(existingByPackageResolved.toPublicBucket())
            }

            val existingByIdentifierDeps = bucketsByIdentifierDepsHash[identifierDepsHash]
            if (existingByIdentifierDeps != null) {
                packageResolvedHash?.let { bucketsByPackageResolvedHash.putIfAbsent(it, existingByIdentifierDeps) }
                return XcodeDumpClaim.Existing(existingByIdentifierDeps.toPublicBucket())
            }

            val bucketId = packageResolvedHash ?: identifierDepsHash
            val newBucket = MutableXcodeDumpBucket(
                id = bucketId,
                sharedDumpDir = sharedDumpRoot.resolve(bucketId),
            )
            packageResolvedHash?.let { bucketsByPackageResolvedHash[it] = newBucket }
            bucketsByIdentifierDepsHash[identifierDepsHash] = newBucket
            return XcodeDumpClaim.Owner(newBucket.toPublicBucket())
        }
    }

    fun awaitXcodeDump(bucket: XcodeDumpBucket) {
        val mutableBucket = synchronized(stateLock) {
            bucket.mutableBucket()
        }
        mutableBucket.completion.await()
        mutableBucket.failure?.let {
            throw GradleException("Shared SwiftPM xcodebuild dump failed for bucket '${bucket.id}'", it)
        }
    }

    fun markXcodeDumpCompleted(bucket: XcodeDumpBucket) {
        val mutableBucket = synchronized(stateLock) {
            bucket.mutableBucket()
        }
        mutableBucket.completion.countDown()
    }

    fun markXcodeDumpFailed(bucket: XcodeDumpBucket, failure: Throwable) {
        val mutableBucket = synchronized(stateLock) {
            bucket.mutableBucket().apply {
                this.failure = failure
            }
        }
        mutableBucket.completion.countDown()
    }

    private fun XcodeDumpBucket.mutableBucket(): MutableXcodeDumpBucket {
        return bucketsByIdentifierDepsHash.values.firstOrNull { it.id == id }
            ?: bucketsByPackageResolvedHash.values.firstOrNull { it.id == id }
            ?: error("Unknown SwiftPM xcodebuild dump bucket '$id'")
    }

    companion object {
        private const val SERVICE_NAME = "swiftPMXcodeDumpBuildService"

        /** Registers the shared service once per build. */
        fun registerIfAbsent(project: Project): Provider<SwiftPMXcodeDumpBuildService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                SwiftPMXcodeDumpBuildService::class.java
            ) {}
    }
}

private data class MutableXcodeDumpBucket(
    val id: String,
    val sharedDumpDir: File,
    var failure: Throwable? = null,
    val completion: CountDownLatch = CountDownLatch(1),
) {
    fun toPublicBucket(): SwiftPMXcodeDumpBuildService.XcodeDumpBucket =
        SwiftPMXcodeDumpBuildService.XcodeDumpBucket(id, sharedDumpDir)
}

internal fun normalizeXcodebuildArgs(args: List<String>): List<String> {
    return args.sorted()
}

internal fun normalizedXcodeDumpTaskFingerprintByPackageResolvedFile(
    packageResolvedFile: File,
): String {
    val packageResolvedHash = MessageDigest.getInstance("SHA-256")
        .digest(packageResolvedFile.readBytes())
        .joinToString("") { byte -> "%02x".format(byte) }

    return packageResolvedHash
}

internal fun normalizedXcodeDumpTaskFingerprintByIdentifierDeps(
    packageResolvedSynchronization: String,
    xcodebuildPlatform: String,
    xcodebuildSdk: String,
    architectures: Set<AppleArchitecture>,
    additionalXcodeArgs: List<String>,
    directSwiftPmDependencies: Set<SwiftPMDependency>,
    transitiveSwiftPmDependencies: TransitiveSwiftPMDependencies,
): String {
    // The fingerprint serializes only build-relevant SwiftPM inputs so equal declarations map to one shared dump owner.
    val payload = dumpTaskFingerprintJson.encodeToString(
        DumpTaskFingerprint(
            packageResolvedSynchronization = packageResolvedSynchronization,
            xcodebuildPlatform = xcodebuildPlatform,
            xcodebuildSdk = xcodebuildSdk,
            architectures = architectures.map { it.name }.sorted(),
            directSwiftPmDependencies = directSwiftPmDependencies
                .map { it.toDumpTaskFingerprint() }
                .sortedBy { it.stableSortKey },
            transitiveSwiftPmDependencies = transitiveSwiftPmDependencies.metadataByDependencyIdentifier
                .values
                .map { metadata ->
                    NormalizedTransitiveSwiftPMMetadata(
                        stableSortKey = buildString {
                            append(metadata.konanTargets.sorted().joinToString(","))
                            append('|')
                            append(metadata.iosDeploymentVersion.orEmpty())
                            append('|')
                            append(metadata.macosDeploymentVersion.orEmpty())
                            append('|')
                            append(metadata.watchosDeploymentVersion.orEmpty())
                            append('|')
                            append(metadata.tvosDeploymentVersion.orEmpty())
                            append('|')
                            append(
                                metadata.dependencies
                                    .map { it.toDumpTaskFingerprint() }
                                    .sortedBy { it.stableSortKey }
                                    .joinToString(";") { it.stableSortKey }
                            )
                        },
                        konanTargets = metadata.konanTargets.sorted(),
                        iosDeploymentTarget = metadata.iosDeploymentVersion,
                        macosDeploymentTarget = metadata.macosDeploymentVersion,
                        watchosDeploymentTarget = metadata.watchosDeploymentVersion,
                        tvosDeploymentTarget = metadata.tvosDeploymentVersion,
                        dependencies = metadata.dependencies
                            .map { it.toDumpTaskFingerprint() }
                            .sortedBy { it.stableSortKey },
                    )
                }
                .sortedBy { it.stableSortKey },
            additionalXcodeArgs = normalizeXcodebuildArgs(additionalXcodeArgs),
        )
    )

    return MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}

@Serializable
private data class DumpTaskFingerprint(
    val packageResolvedSynchronization: String,
    val xcodebuildPlatform: String,
    val xcodebuildSdk: String,
    val architectures: List<String>,
    val directSwiftPmDependencies: List<NormalizedSwiftPMDependency>,
    val transitiveSwiftPmDependencies: List<NormalizedTransitiveSwiftPMMetadata>,
    val additionalXcodeArgs: List<String>,
)

internal fun PackageResolvedSynchronization.toDumpTaskFingerprint(): String = when (this) {
    is PackageResolvedSynchronization.Identifier -> "identifier:$identifier"
    PackageResolvedSynchronization.None -> "none"
}

@Serializable
private data class NormalizedSwiftPMDependency(
    val stableSortKey: String,
    val kind: String,
    val packageName: String,
    val traits: List<String>,
    val products: List<NormalizedSwiftPMProduct>,
    val location: String? = null,
    val version: String? = null,
)

@Serializable
private data class NormalizedSwiftPMProduct(
    val name: String,
    val platformConstraints: List<String>,
)

@Serializable
private data class NormalizedTransitiveSwiftPMMetadata(
    val stableSortKey: String,
    val konanTargets: List<String>,
    val iosDeploymentTarget: String?,
    val macosDeploymentTarget: String?,
    val watchosDeploymentTarget: String?,
    val tvosDeploymentTarget: String?,
    val dependencies: List<NormalizedSwiftPMDependency>,
)

private fun SwiftPMDependency.toDumpTaskFingerprint(): NormalizedSwiftPMDependency = when (this) {
    is SwiftPMDependency.Local -> NormalizedSwiftPMDependency(
        stableSortKey = "local|${absolutePath.path}|$packageName",
        kind = "local",
        packageName = packageName,
        traits = traits.sorted(),
        products = products.map { it.toDumpTaskFingerprint() }.sortedBy { it.name },
        location = absolutePath.path,
    )
    is SwiftPMDependency.Remote -> {
        val normalizedRepository = repository.toDumpTaskFingerprint()
        val normalizedVersion = version.toDumpTaskFingerprint()
        NormalizedSwiftPMDependency(
            stableSortKey = "remote|$normalizedRepository|$normalizedVersion|$packageName",
            kind = "remote",
            packageName = packageName,
            traits = traits.sorted(),
            products = products.map { it.toDumpTaskFingerprint() }.sortedBy { it.name },
            location = normalizedRepository,
            version = normalizedVersion,
        )
    }
}

private fun SwiftPMDependency.Product.toDumpTaskFingerprint(): NormalizedSwiftPMProduct =
    NormalizedSwiftPMProduct(
        name = name,
        platformConstraints = platformConstraints.orEmpty().map { it.name }.sorted(),
    )

private fun SwiftPMDependency.Remote.Repository.toDumpTaskFingerprint(): String = when (this) {
    is SwiftPMDependency.Remote.Repository.Id -> "id:$value"
    is SwiftPMDependency.Remote.Repository.Url -> "url:$value"
}

private fun SwiftPMDependency.Remote.Version.toDumpTaskFingerprint(): String = when (this) {
    is SwiftPMDependency.Remote.Version.Exact -> "exact:$value"
    is SwiftPMDependency.Remote.Version.From -> "from:$value"
    is SwiftPMDependency.Remote.Version.Range -> "range:$from..$through"
    is SwiftPMDependency.Remote.Version.Branch -> "branch:$value"
    is SwiftPMDependency.Remote.Version.Revision -> "revision:$value"
}

private val dumpTaskFingerprintJson = Json {
    encodeDefaults = true
}
