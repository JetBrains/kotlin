/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
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
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.String
import kotlin.collections.List


/**
 * These keys cannot be calculated safely during configuration because transitive SwiftPM metadata and local package
 * inputs are produced by [SerializeSwiftPMDependenciesMetadata]. Keeping this as a task makes those generated files regular Gradle inputs, while
 * the fingerprint task can stay simple: it reads the prepared JSON and asks [SwiftImportFingerprintedCoordinationService] whether to own or
 * join a shared swift package fetch bucket.
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class FingerprintSyntheticPackage : DefaultTask() {
    @get:Internal
    abstract val transitiveSwiftPMMetadata: Property<TransitiveSwiftPMMetadata>

    @get:Internal
    abstract val directSwiftPMMetadata: Property<SwiftPMImportMetadata>

    @Suppress("unused")
    @Serializable
    private class JsonFingerprintWrapper(
        val transitiveDependencies: TransitiveSwiftPMMetadata,
        val directSwiftPMDependencies: SwiftPMImportMetadata,
    )

    /**
     * This was needed to for UDT check for transitive dependencies and metadata properties above.
     * The @Input annotation with java.io.Serializable for those was causing serialization "Deprecation" issues for Gradle 7.6.3,
     * so they are marked as @Internal, and this synthetic input is computed using Kotlinx serialization instead.
     */
    @get:Input
    protected val dependencyGraphFingerprintInput: Provider<String> =
        transitiveSwiftPMMetadata.zip(directSwiftPMMetadata) { transitive, direct ->
            json.encodeToString(
                JsonFingerprintWrapper(
                    transitive,
                    direct,
                )
            )
        }


    /** Normalized Package.resolved synchronization mode. This is part of the diagnostic identifier/dependencies key. */
    @get:Input
    abstract val packageResolvedSynchronizationFingerprint: Property<PackageResolvedSynchronization>

    @get:OutputFile
    val syntheticPackageFingerprint: Provider<RegularFile> =
        project.objects.fileProperty().convention(
            project.layout.buildDirectory.file(
                SYNTHETIC_PACKAGE_FINGERPRINT_PATH
            )
        )

    @TaskAction
    fun fingerprint() {
        val withVersions = fingerprintSwiftPMDependencyGraph(
            directSwiftPMMetadata.get(), transitiveSwiftPMMetadata.get(),
            normalizeVersions = false
        )
        val withoutVersions = fingerprintSwiftPMDependencyGraph(
            directSwiftPMMetadata.get(), transitiveSwiftPMMetadata.get(),
            normalizeVersions = true
        )

        val calculatedSyntheticPackageFingerprintWithVersions = fingerprintSyntheticPackage(
            packageResolvedSynchronizationFingerprint = packageResolvedSynchronizationFingerprint.get().toKotlinxSerializable(),
            fingerprintedDependencyGraph = withVersions
        )
        val calculatedSyntheticPackageFingerprintWithoutVersions = fingerprintSyntheticPackage(
            packageResolvedSynchronizationFingerprint = packageResolvedSynchronizationFingerprint.get().toKotlinxSerializable(),
            fingerprintedDependencyGraph = withoutVersions
        )

        dumpFingerprint(calculatedSyntheticPackageFingerprintWithVersions + "\n" + calculatedSyntheticPackageFingerprintWithoutVersions, syntheticPackageFingerprint.get().asFile)
    }

    companion object {
        const val TASK_NAME = "fingerprintSyntheticPackage"
        const val SYNTHETIC_PACKAGE_FINGERPRINT_PATH = "kotlin/syntheticPackageFingerprint"
        protected val json = Json {
            allowStructuredMapKeys = true
        }
    }
}

/**
 * Prepares the execution-time sharing keys for [DumpXcodeBuildArgs].
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class FingerprintXcodeBuild : DefaultTask() {

    /** SDK name passed to xcodebuild. It also determines the SDK-specific dump and DerivedData subdirectories. */
    @get:Input
    abstract val xcodebuildSdk: Property<String>

    /** Architectures requested from xcodebuild. The dump output is architecture-sensitive. */
    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    /** Extra xcodebuild arguments that affect package resolution or the generated build invocation. */
    @get:Input
    val additionalXcodeArgs: ListProperty<String> = project.objects.listProperty(String::class.java).convention(emptyList())

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val syntheticPackageFingerprint: RegularFileProperty

    private val layout = project.layout

    @get:OutputFile
    val xcodebuildFingerprint: Provider<RegularFile> =
        xcodebuildSdk.map { sdk ->
            layout.buildDirectory.file(
                xcodebuildFingerprintPathForSdk(sdk)
            ).get()
        }

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun fingerprint() {
        val packageFingerprint = syntheticPackageFingerprint.get().asFile.readText().trim().split("\n")
        val withVersion = packageFingerprint[0]
        val withoutVersion = packageFingerprint[1]

        val fingerprintWithVersion = fingerprintXcodebuildFingerprintInput(
            architectures = architectures.get(),
            additionalXcodeArgs = additionalXcodeArgs.get(),
            syntheticPackageFingerprint = withVersion,
        )
        val fingerprintWithoutVersion = fingerprintXcodebuildFingerprintInput(
            architectures = architectures.get(),
            additionalXcodeArgs = additionalXcodeArgs.get(),
            syntheticPackageFingerprint = withoutVersion,
        )

        // The dump task reads this hash during its own execution and performs the build-service claim/join step.
        dumpFingerprint(fingerprintWithVersion + "\n" + fingerprintWithoutVersion, xcodebuildFingerprint.get().asFile)
    }

    companion object {
        const val TASK_NAME = "fingerprintXcodebuild"
        const val XCODEBUILD_FINGERPRINT_PATH = "kotlin/xcodebuildFingerprints"
        fun xcodebuildFingerprintPathForSdk(sdk: String): String = "$XCODEBUILD_FINGERPRINT_PATH/$sdk"
    }
}

internal fun fingerprintXcodebuildFingerprintInput(
    architectures: Set<AppleArchitecture>,
    additionalXcodeArgs: List<String>,
    syntheticPackageFingerprint: String,
): String {
    val payload = dumpTaskFingerprintJson.encodeToString(
        XcodebuildFingerprintInput(
            architectures = architectures.map { it.name }.sorted(),
            syntheticPackageFingerprint = syntheticPackageFingerprint,
            additionalXcodeArgs = additionalXcodeArgs,
        )
    )

    return sha256(payload)
}

private fun dumpFingerprint(fingerprint: String, outputFile: File) {
    outputFile.parentFile.mkdirs()
    outputFile.writeText(fingerprint)
}

@Serializable
private data class XcodebuildFingerprintInput(
//    val packageResolvedSynchronization: String,
    val architectures: List<String>,
    val syntheticPackageFingerprint: String,
    val additionalXcodeArgs: List<String>,
)

@Serializable
private data class SyntheticPackageFingerprintInput(
    val packageResolvedSynchronizationFingerprint: SerializablePackageResolvedSynchronization,
    val dependencyGraphFingerprints: List<String>,
)

internal fun fingerprintSyntheticPackage(
    packageResolvedSynchronizationFingerprint: SerializablePackageResolvedSynchronization,
    fingerprintedDependencyGraph: TransitiveSwiftPMMetadata,
): String {
    val payload = dumpTaskFingerprintJson.encodeToString(
        SyntheticPackageFingerprintInput(
            packageResolvedSynchronizationFingerprint = packageResolvedSynchronizationFingerprint,
            dependencyGraphFingerprints = fingerprintedDependencyGraph.metadataByDependencyIdentifier.keys
                .map { it.identifier }
                .sorted(),
        )
    )

    // FIXME: Document SwiftPM not creating lock file in 4ef5a7d75edf289a2f51a82e7ef2170309bd34d00c3523372cf73499c7dbe137
    return sha256(payload)
}

/**
 * We take in direct and transitive SwiftPM metadata, normalize these by erasing identity such as project names and sorting collections like
 * the imported products list, and produce a Map which is the main fingerprinting for joining package generation and the fetching of the
 * package. This is also used for generating the normalized package itself, so that project names become hashes of their SwiftPM metadata.
 */
internal fun fingerprintSwiftPMDependencyGraph(
    directSwiftPMMetadata: SwiftPMImportMetadata,
    transitiveSwiftPMMetadata: TransitiveSwiftPMMetadata,
    normalizeVersions: Boolean,
): TransitiveSwiftPMMetadata {
    val transformed = linkedMapOf<SwiftPMDependencyIdentifier, SwiftPMImportMetadata>()

    // isModular is only used for FUS, so for fingerprinting we ignore it
    if (directSwiftPMMetadata.dependencies.isNotEmpty()) {
        val hash = fingerprintSwiftPMImportMetadata(directSwiftPMMetadata, normalizeVersions)
        transformed[SwiftPMDependencyIdentifier(hash, isModular = false)] =
            directSwiftPMMetadata.copy()
    }

    transitiveSwiftPMMetadata.metadataByDependencyIdentifier.entries
        .sortedBy { it.key.identifier }
        .forEach { (dependencyIdentifier, metadata) ->
            if (metadata.dependencies.isEmpty()) return@forEach

            val hash = fingerprintSwiftPMImportMetadata(metadata, normalizeVersions)

            transformed.putIfAbsent(
                SwiftPMDependencyIdentifier(hash, isModular = dependencyIdentifier.isModular),
                metadata,
            )
        }

    return TransitiveSwiftPMMetadata(transformed)
}

internal fun fingerprintSwiftPMImportMetadata(
    metadata: SwiftPMImportMetadata,
    normalizeVersions: Boolean,
): String {
    val payload = dumpTaskFingerprintJson.encodeToString(
        SwiftPMImportMetadataFingerprintInput(
            konanTargets = metadata.konanTargets.sorted(),
            iosDeploymentVersion = metadata.iosDeploymentVersion,
            macosDeploymentVersion = metadata.macosDeploymentVersion,
            watchosDeploymentVersion = metadata.watchosDeploymentVersion,
            tvosDeploymentVersion = metadata.tvosDeploymentVersion,
            isModulesDiscoveryEnabled = false,
            dependencies = metadata.dependencies
                .map { it.normalizedForFingerprint(normalizeVersions) }
                .sortedBy { it.stableSortKey() },
        )
    )
    return sha256(payload)
}

internal const val SWIFT_IMPORT_HASH_ALGORITHM = "SHA-256"


private fun sha256(value: String): String =
    sha256(value.toByteArray()).substring(0, 10)

private fun sha256(bytes: ByteArray): String {
    return MessageDigest.getInstance(SWIFT_IMPORT_HASH_ALGORITHM)
        .digest(bytes)
        .toHexString()
}

private fun ByteArray.toHexString(): String =
    joinToString("") { "%02x".format(it) }

private val dumpTaskFingerprintJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}


internal fun fingerprintLocalPackageSources(
    files: List<File>,
): String {
    val digest = MessageDigest.getInstance(SWIFT_IMPORT_HASH_ALGORITHM)
    files
        // The tracking file can contain both individual files and source directories. Directories are expanded here so
        // source edits, additions, and removals change the dump sharing key.
        .flatMap { trackedFile ->
            when {
                trackedFile.isFile -> listOf(trackedFile to trackedFile.name)
                trackedFile.isDirectory -> trackedFile.walkTopDown().filter { it.isFile }
                    .map { file -> file to file.relativeTo(trackedFile).invariantSeparatorsPath }.toList()
                else -> emptyList()
            }
        }
        // Stable ordering keeps the hash independent of filesystem traversal order.
        .sortedBy { (_, relativePath) -> relativePath }.forEach { (file, relativePath) ->
            // Include both relative path and content. The zero byte separators avoid accidental concatenation
            // collisions between neighboring entries.
            digest.update(relativePath.toByteArray())
            digest.update(0.toByte())
            digest.update(file.readBytes())
            digest.update(0.toByte())
        }

    return digest.digest().toHexString()
}

@Serializable
private data class SwiftPMImportMetadataFingerprintInput(
    val konanTargets: List<String>,
    val iosDeploymentVersion: String?,
    val macosDeploymentVersion: String?,
    val watchosDeploymentVersion: String?,
    val tvosDeploymentVersion: String?,
    val isModulesDiscoveryEnabled: Boolean,
    val dependencies: List<SwiftPMDependency>,
)


internal fun fingerprintPackageResolvedSynchronization(
    packageResolvedSynchronization: SerializablePackageResolvedSynchronization,
): String = sha256(
    dumpTaskFingerprintJson.encodeToString(
        packageResolvedSynchronization
    )
)

private fun SwiftPMDependency.normalizedForFingerprint(normalizeVersions: Boolean): SwiftPMDependency = when (this) {
    is SwiftPMDependency.Local -> copy(
        products = products.map { it.normalizedForFingerprint() }.sortedBy { it.name },
        cinteropClangModules = cinteropClangModules.map { it.normalizedForFingerprint() }.sortedBy { it.stableSortKey() },
        traits = traits.sorted().toSet(),
    )

    is SwiftPMDependency.Remote -> copy(
        products = products.map { it.normalizedForFingerprint() }.sortedBy { it.name },
        cinteropClangModules = cinteropClangModules.map { it.normalizedForFingerprint() }.sortedBy { it.stableSortKey() },
        traits = traits.sorted().toSet(),
        // FIXME:
        version = if (normalizeVersions) SwiftPMDependency.Remote.Version.Exact("1.0.0") else version,
    )
}

private fun SwiftPMDependency.Product.normalizedForFingerprint(): SwiftPMDependency.Product =
    copy(
        cinteropClangModules = cinteropClangModules.sorted().toSet(),
        platformConstraints = platformConstraints?.sortedBy { it.name }?.toSet(),
    )

private fun SwiftPMDependency.CinteropClangModule.normalizedForFingerprint(): SwiftPMDependency.CinteropClangModule =
    copy(
        platformConstraints = platformConstraints?.sortedBy { it.name }?.toSet(),
    )

private fun SwiftPMDependency.stableSortKey(): String = when (this) {
    is SwiftPMDependency.Local ->
        "local|${absolutePath.path}|$packageName"
    is SwiftPMDependency.Remote ->
        "remote|${repository.stableSortKey()}|${version.stableSortKey()}|$packageName"
}

private fun SwiftPMDependency.CinteropClangModule.stableSortKey(): String =
    "$name|${platformConstraints.orEmpty().map { it.name }.sorted().joinToString(",")}"

private fun SwiftPMDependency.Remote.Repository.stableSortKey(): String = when (this) {
    is SwiftPMDependency.Remote.Repository.Id -> "id:$value"
    is SwiftPMDependency.Remote.Repository.Url -> "url:$value"
}

private fun SwiftPMDependency.Remote.Version.stableSortKey(): String = when (this) {
    is SwiftPMDependency.Remote.Version.Exact -> "exact:$value"
    is SwiftPMDependency.Remote.Version.From -> "from:$value"
    is SwiftPMDependency.Remote.Version.Range -> "range:$from..$through"
    is SwiftPMDependency.Remote.Version.Branch -> "branch:$value"
    is SwiftPMDependency.Remote.Version.Revision -> "revision:$value"
}
