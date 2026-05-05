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

    @get:OutputDirectory
    abstract val dumpedXcodeBuildArgsDir: DirectoryProperty

    @get:OutputDirectory
    val syntheticImportDd: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir(XcodebuildDefFileUtils.SYNTHETIC_IMPORT_DD_DIR)
    )

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

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

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
            val fingerprints = dumpTaskFingerprintJson.decodeFromString<XcodeDumpSharingFingerprints>(
                fingerprintsFile.get().asFile.readText()
            )

            val claim = coordinationService.get().claimOrJoinXcodeDump(
                packageResolvedHash = fingerprints.packageResolvedHash,
                identifierDepsHash = fingerprints.identifierDepsHash,
                sdkDerivedDataDirName = "dd_${xcodebuildSdk.get()}",
                ownerDumpDir = dumpedXcodeBuildArgsDir.get().asFile,
                ownerDerivedDataDir = syntheticImportDd.get().asFile,
                syntheticImportProjectRoot = syntheticImportProjectRoot.get().asFile,
                swiftPMDependenciesCheckout = swiftPMDependenciesCheckout.get().asFile,
            )

            when (claim) {
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Owner -> runOwnerXcodeDump(claim.bucket)
                is SwiftPMXcodeDumpBuildService.XcodeDumpClaim.Existing -> coordinationService.get().awaitXcodeDump(claim.bucket)
            }

            copyOwnerDumpToLocalOutput(claim.bucket)
            copyOwnerDerivedDataToLocalOutput(claim.bucket)
        }
    }

    private fun runOwnerXcodeDump(bucket: SwiftPMXcodeDumpBuildService.XcodeDumpBucket) {
        try {
            submitXcodebuildArgsDumpWorkAction(
                ownerDumpDir = bucket.ownerDumpDir,
                ownerDerivedDataDir = bucket.ownerDerivedDataDir,
            )
            workerExecutor.await()
            coordinationService.get().markXcodeDumpCompleted(bucket)
        } catch (failure: Throwable) {
            coordinationService.get().markXcodeDumpFailed(bucket, failure)
            throw failure
        }
    }

    private fun submitXcodebuildArgsDumpWorkAction(
        ownerDumpDir: File,
        ownerDerivedDataDir: File,
    ) {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
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
        }
    }

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
        val pathBoundary = "(?=$|[/;\\s\"'])"
        return Regex("${Regex.escape(from)}$pathBoundary").replace(this, Regex.escapeReplacement(to))
    }

    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
    }
}

internal fun normalizeXcodebuildArgs(args: List<String>): List<String> {
    return args.sorted()
}

internal fun normalizedXcodeDumpTaskFingerprintByPackageResolvedFile(
    packageResolvedFile: File,
    xcodebuildPlatform: String,
    xcodebuildSdk: String,
    architectures: Set<AppleArchitecture>,
    additionalXcodeArgs: List<String>,
    buildSettingsFingerprint: String,
): String {
    val packageResolvedHash = MessageDigest.getInstance("SHA-256")
        .digest(packageResolvedFile.readBytes())
        .joinToString("") { byte -> "%02x".format(byte) }

    val payload = dumpTaskFingerprintJson.encodeToString(
        PackageResolvedDumpTaskFingerprint(
            packageResolvedHash = packageResolvedHash,
            xcodebuildPlatform = xcodebuildPlatform,
            xcodebuildSdk = xcodebuildSdk,
            architectures = architectures.map { it.name }.sorted(),
            buildSettingsFingerprint = buildSettingsFingerprint,
            additionalXcodeArgs = normalizeXcodebuildArgs(additionalXcodeArgs),
        )
    )

    return MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}

internal fun normalizedXcodeDumpTaskFingerprintByIdentifierDeps(
    packageResolvedSynchronization: String,
    xcodebuildPlatform: String,
    xcodebuildSdk: String,
    architectures: Set<AppleArchitecture>,
    additionalXcodeArgs: List<String>,
    directSwiftPmDependencies: Set<SwiftPMDependency>,
    transitiveSwiftPmDependencies: TransitiveSwiftPMDependencies,
    buildSettingsFingerprint: String,
): String {
    // The fingerprint serializes only build-relevant SwiftPM inputs so equal declarations map to one shared dump owner.
    val payload = dumpTaskFingerprintJson.encodeToString(
        DumpTaskFingerprint(
            packageResolvedSynchronization = packageResolvedSynchronization,
            xcodebuildPlatform = xcodebuildPlatform,
            xcodebuildSdk = xcodebuildSdk,
            architectures = architectures.map { it.name }.sorted(),
            buildSettingsFingerprint = buildSettingsFingerprint,
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
    val buildSettingsFingerprint: String,
    val directSwiftPmDependencies: List<NormalizedSwiftPMDependency>,
    val transitiveSwiftPmDependencies: List<NormalizedTransitiveSwiftPMMetadata>,
    val additionalXcodeArgs: List<String>,
)

@Serializable
private data class PackageResolvedDumpTaskFingerprint(
    val packageResolvedHash: String,
    val xcodebuildPlatform: String,
    val xcodebuildSdk: String,
    val architectures: List<String>,
    val buildSettingsFingerprint: String,
    val additionalXcodeArgs: List<String>,
)

internal fun SwiftPMImportExtension.dumpTaskBuildSettingsFingerprint(): String = dumpTaskFingerprintJson.encodeToString(
    DumpTaskBuildSettingsFingerprint(
        iosDeploymentTarget = iosMinimumDeploymentTarget.orNull.orEmpty(),
        macosDeploymentTarget = macosMinimumDeploymentTarget.orNull.orEmpty(),
        watchosDeploymentTarget = watchosMinimumDeploymentTarget.orNull.orEmpty(),
        tvosDeploymentTarget = tvosMinimumDeploymentTarget.orNull.orEmpty(),
    )
)

@Serializable
private data class DumpTaskBuildSettingsFingerprint(
    val iosDeploymentTarget: String,
    val macosDeploymentTarget: String,
    val watchosDeploymentTarget: String,
    val tvosDeploymentTarget: String,
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
