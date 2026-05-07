/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import java.security.MessageDigest
import kotlin.collections.flatMap

/**
 * Prepares the execution-time sharing keys for [DumpXcodeBuildArgs].
 *
 * These keys cannot be calculated safely during configuration because Package.resolved and transitive SwiftPM metadata
 * are produced by other tasks. Keeping this as a task makes those generated files regular Gradle inputs, while the dump
 * task can stay simple: it reads the prepared JSON and asks [SwiftPMXcodeDumpBuildService] whether to own or join a
 * shared xcodebuild dump bucket.
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class PrepareXcodeBuildArgsDumpFingerprint : DefaultTask() {
    /** Xcode destination platform, for example `iOS` or `iOS Simulator`. Different platforms cannot share dumps. */
    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    /** SDK name passed to xcodebuild. It also determines the SDK-specific DerivedData subdirectory. */
    @get:Input
    abstract val xcodebuildSdk: Property<String>

    /** Architectures requested from xcodebuild. The dump output is architecture-sensitive. */
    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    /** Extra xcodebuild arguments that affect package resolution or the generated build invocation. */
    @get:Input
    val additionalXcodeArgs: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(emptyList())

    /** Normalized Package.resolved synchronization mode. This is part of the fallback key. */
    @get:Input
    abstract val packageResolvedSynchronization: Property<String>

    /** Direct SwiftPM dependencies declared in the current project. */
    @get:Input
    abstract val directSwiftPMDependencies: SetProperty<SwiftPMDependency>

    /**
     * Stable Gradle input for SwiftPM dependencies coming from project dependencies.
     *
     * The raw [TransitiveSwiftPMDependencies] object has a generated equals implementation that can trigger Gradle's
     * "different serialized form but equal" deprecation under `--warning-mode=fail`. Expose the normalized string that
     * is actually used by the dump fingerprint instead.
     */
    @get:Input
    abstract val transitiveSwiftPMDependenciesFingerprint: Property<String>

    /** SwiftPM dependencies coming from project dependencies, used at execution time to build the final dump key. */
    @get:Internal
    abstract val transitiveSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>

    /**
     * Synthetic-project settings that can change xcodebuild output without changing SwiftPM dependencies.
     *
     * For example, deployment targets are written into generated Package.swift rather than passed as explicit
     * xcodebuild args, but they can still change target triples in captured clang/linker invocations.
     */
    @get:Input
    abstract val buildSettingsFingerprint: Property<String>

    /**
     * Files produced/used by SwiftPM package resolution.
     *
     * This makes the fingerprint task rerun when fetch changes Package.resolved or relevant manifests. The actual
     * Package.resolved file is still read through [packageResolvedFile] so we can treat absence as "no exact key".
     */
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    /**
     * Generated Package.resolved file used for the preferred exact sharing key.
     *
     * It is internal because [resolvedPackagesState] already declares the Gradle input. Keeping this property internal
     * lets the task handle missing Package.resolved gracefully instead of failing validation before execution.
     */
    @get:Internal
    abstract val packageResolvedFile: RegularFileProperty

    /** File listing local package manifests/source roots that should invalidate a dump when local sources change. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    /**
     * Actual local package inputs derived from [filesToTrackFromLocalPackages].
     *
     * SwiftPM local package source changes can change generated module maps and Objective-C headers without changing
     * Package.resolved. They therefore have to contribute to the sharing key, otherwise a later build could reuse a
     * stale persisted xcodebuild dump.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val localPackageSources: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map {
            it.asFile.readLines().filter { line -> line.isNotEmpty() }.map { line -> File(line) }
        }

    @get:OutputFile
    abstract val fingerprintsFile: RegularFileProperty

    @TaskAction
    fun prepareFingerprint() {
        // Bundle synthetic-project settings and local source content into one string so both matching strategies include
        // them. This keeps exact Package.resolved matching safe when the lock file stays unchanged but generated project
        // contents, target triples, module maps, or local-package headers can change.
        val buildSettingsAndLocalPackagesFingerprint = dumpTaskFingerprintJson.encodeToString(
            DumpTaskBuildSettingsAndLocalPackagesFingerprint(
                buildSettingsFingerprint = buildSettingsFingerprint.get(),
                localPackageSourcesFingerprint = localPackageSourcesFingerprint(localPackageSources.get()),
            )
        )
        // Fallback key used when no exact Package.resolved key is available or when the resolved lock differs but the
        // declared graph/build inputs are equivalent enough for sharing.
        val identifierDepsFingerprint = normalizedXcodeDumpTaskFingerprintByIdentifierDeps(
            packageResolvedSynchronization.get(),
            xcodebuildPlatform.get(),
            xcodebuildSdk.get(),
            architectures.get(),
            additionalXcodeArgs.get(),
            directSwiftPMDependencies.get(),
            transitiveSwiftPMDependencies.get(),
            buildSettingsAndLocalPackagesFingerprint,
        )
        // Preferred key. If Package.resolved exists, tasks resolving to the same lock file can share even when their
        // synchronization identifiers differ.
        val packageResolvedFingerprint = packageResolvedFile.orNull
            ?.asFile
            ?.takeIf { it.exists() }
            ?.let {
                normalizedXcodeDumpTaskFingerprintByPackageResolvedFile(
                    packageResolvedFile = it,
                    xcodebuildPlatform = xcodebuildPlatform.get(),
                    xcodebuildSdk = xcodebuildSdk.get(),
                    architectures = architectures.get(),
                    additionalXcodeArgs = additionalXcodeArgs.get(),
                    buildSettingsFingerprint = buildSettingsAndLocalPackagesFingerprint,
                )
            }

        // The dump task reads this JSON during its own execution and performs the build-service claim/join step.
        val output = fingerprintsFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            dumpTaskFingerprintJson.encodeToString(
                XcodeDumpSharingFingerprints(
                    packageResolvedHash = packageResolvedFingerprint,
                    identifierDepsHash = identifierDepsFingerprint,
                )
            )
        )
    }

    companion object {
        const val TASK_NAME = "prepareXcodebuildArgsDumpFingerprint"
    }
}


internal fun localPackageSourcesFingerprint(
    files: List<File>,
): String {
    val digest = MessageDigest.getInstance("SHA-256")
    files
        // The tracking file can contain both individual files and source directories. Directories are expanded here so
        // source edits, additions, and removals change the dump sharing key.
        .flatMap { trackedFile ->
            when {
                trackedFile.isFile -> listOf(trackedFile to trackedFile.name)
                trackedFile.isDirectory -> trackedFile.walkTopDown()
                    .filter { it.isFile }
                    .map { file -> file to file.relativeTo(trackedFile).invariantSeparatorsPath }
                    .toList()
                else -> emptyList()
            }
        }
        // Stable ordering keeps the hash independent of filesystem traversal order.
        .sortedBy { (_, relativePath) -> relativePath }
        .forEach { (file, relativePath) ->
            // Include both relative path and content. The zero byte separators avoid accidental concatenation
            // collisions between neighboring entries.
            digest.update(relativePath.toByteArray())
            digest.update(0.toByte())
            digest.update(file.readBytes())
            digest.update(0.toByte())
        }

    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

internal fun normalizeXcodebuildArgs(args: List<String>): List<String> {
    // Tests can configure these args through providers in different orders. Sorting avoids splitting buckets only
    // because semantically identical optional xcodebuild arguments were declared in a different order.
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
    // Hash normalized lock contents first, then combine that hash with the platform/build inputs that affect dump output.
    // SwiftPM can preserve either `https://host/repo` or `https://host/repo.git` in Package.resolved for the same pin,
    // depending on the existing lock source. Normalizing `location` avoids splitting dump buckets on that spelling.
    val packageResolvedHash = MessageDigest.getInstance("SHA-256")
        .digest(normalizedPackageResolvedContentForFingerprint(packageResolvedFile).toByteArray())
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

private fun normalizedPackageResolvedContentForFingerprint(packageResolvedFile: File): String {
    val packageResolvedText = packageResolvedFile.readText()

    return dumpTaskFingerprintJson.encodeToString(
        dumpTaskFingerprintJson.decodeFromString<SwiftPackageResolved>(packageResolvedText)
            .withoutGitSuffixInLocations()
    )
}

private fun SwiftPackageResolved.withoutGitSuffixInLocations(): SwiftPackageResolved =
    copy(
        pins = pins
            .map { pin ->
                // GitHub HTTPS remotes are accepted by SwiftPM both as `https://host/org/repo` and
                // `https://host/org/repo.git`. SwiftPM can keep either spelling in Package.resolved depending on which
                // lock file was used as the starting point, even when identity/revision/version are identical. The
                // xcodebuild output is not affected by this suffix, so the sharing fingerprint removes it to avoid
                // unnecessary duplicate xcodebuild runs.
                pin.copy(location = pin.location.removeSuffix(".git"))
            }
            // SwiftPM normally writes pins in a stable order, but the fingerprint does not depend on that order.
            // Sorting avoids duplicate xcodebuild runs if equivalent Package.resolved files differ only by pin order.
            .sortedBy { it.identity }
    )

@Serializable
private data class SwiftPackageResolved(
    val pins: List<SwiftPackageResolvedPin>,
    val version: Int,
)

@Serializable
private data class SwiftPackageResolvedPin(
    val identity: String,
    val kind: String,
    val location: String,
    val state: JsonObject,
)

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
    // Every collection is normalized before serialization to keep the key stable across Gradle/provider iteration order.
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
            transitiveSwiftPmDependencies = transitiveSwiftPmDependencies.normalizedTransitiveSwiftPMMetadata(),
            additionalXcodeArgs = normalizeXcodebuildArgs(additionalXcodeArgs),
        )
    )

    return MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}

internal fun TransitiveSwiftPMDependencies.toDumpTaskFingerprintInput(): String =
    dumpTaskFingerprintJson.encodeToString(normalizedTransitiveSwiftPMMetadata())

private fun TransitiveSwiftPMDependencies.normalizedTransitiveSwiftPMMetadata(): List<NormalizedTransitiveSwiftPMMetadata> =
    metadataByDependencyIdentifier
        .values
        .map { metadata ->
            NormalizedTransitiveSwiftPMMetadata(
                // The stable sort key is only for deterministic ordering of transitive metadata. The serialized object
                // below still contains the structured values used by the actual fingerprint payload.
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
        .sortedBy { it.stableSortKey }

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
    // These values affect the generated synthetic Package.swift platforms block. They are intentionally fingerprinted
    // even though they are not direct xcodebuild command-line arguments.
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

@Serializable
private data class DumpTaskBuildSettingsAndLocalPackagesFingerprint(
    val buildSettingsFingerprint: String,
    val localPackageSourcesFingerprint: String,
)
