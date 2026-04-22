/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Prepares the execution-time sharing keys for [DumpXcodeBuildArgs].
 *
 * These keys cannot be calculated safely during configuration because transitive SwiftPM metadata and local package
 * inputs are produced by other tasks. Keeping this as a task makes those generated files regular Gradle inputs, while
 * the dump task can stay simple: it reads the prepared JSON and asks [SwiftPMXcodeDumpBuildService] whether to own or
 * join a shared xcodebuild dump bucket.
 */
@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class PrepareXcodeBuildArgsDumpFingerprint : DefaultTask() {

    /** SDK name passed to xcodebuild. It also determines the SDK-specific dump and DerivedData subdirectories. */
    @get:Input
    abstract val xcodebuildSdk: Property<String>

    /** Architectures requested from xcodebuild. The dump output is architecture-sensitive. */
    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    /** Extra xcodebuild arguments that affect package resolution or the generated build invocation. */
    @get:Input
    val additionalXcodeArgs: ListProperty<String> = project.objects.listProperty(String::class.java).convention(emptyList())

    /** Normalized Package.resolved synchronization mode. This is part of the diagnostic identifier/dependencies key. */
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
    abstract val normalizedTransitiveSwiftPMDependenciesInput: Property<String>

    /**
     * Synthetic-project settings that can change xcodebuild output without changing SwiftPM dependencies.
     *
     * For example, deployment targets are written into generated Package.swift rather than passed as explicit
     * xcodebuild args, but they can still change target triples in captured clang/linker invocations.
     */
    @get:Input
    abstract val buildSettingsFingerprint: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val localPackageSources: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map {
            it.asFile.readLines().filter { line -> line.isNotEmpty() }.map { line -> File(line) }
        }

    @get:OutputFile
    val xcodebuildExecutionHashFile: RegularFileProperty =
        project.objects.fileProperty().convention(
            xcodebuildSdk.flatMap { sdk ->
                project.layout.buildDirectory.file(
                    "kotlin/swiftPMXcodeBuildExecutionHashes/$sdk"
                )
            }
        )

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun prepareFingerprint() {
        workerExecutor.noIsolation().submit(PrepareXcodebuildArgsDumpFingerprintWorkAction::class.java) {
            it.architectures.set(architectures)
            it.additionalXcodeArgs.set(additionalXcodeArgs)
            it.packageResolvedSynchronization.set(packageResolvedSynchronization)
            it.directSwiftPMDependencies.set(directSwiftPMDependencies)
            it.normalizedTransitiveSwiftPMDependenciesInput.set(normalizedTransitiveSwiftPMDependenciesInput)
            it.buildSettingsFingerprint.set(buildSettingsFingerprint)
            it.localPackageSources.set(localPackageSources)
            it.xcodebuildExecutionHashFile.set(xcodebuildExecutionHashFile)
        }
    }

    companion object {
        const val TASK_NAME = "prepareXcodebuildArgsDumpFingerprint"
    }
}

private interface PrepareXcodebuildArgsDumpFingerprintParameters : WorkParameters {
    val architectures: SetProperty<AppleArchitecture>
    val additionalXcodeArgs: ListProperty<String>
    val packageResolvedSynchronization: Property<String>
    val directSwiftPMDependencies: SetProperty<SwiftPMDependency>
    val normalizedTransitiveSwiftPMDependenciesInput: Property<String>
    val buildSettingsFingerprint: Property<String>
    val localPackageSources: ListProperty<File>
    val xcodebuildExecutionHashFile: RegularFileProperty
}

private abstract class PrepareXcodebuildArgsDumpFingerprintWorkAction : WorkAction<PrepareXcodebuildArgsDumpFingerprintParameters> {
    override fun execute() {

        val localPackageSources = parameters.localPackageSources.get()
        // Bundle synthetic-project settings and local source content into one string so generated project contents,
        // target triples, module maps, or local-package headers all affect the dump sharing key.
        val buildSettingsAndLocalPackagesFingerprint = dumpTaskFingerprintJson.encodeToString(
            DumpTaskBuildSettingsAndLocalPackagesFingerprint(
                buildSettingsFingerprint = parameters.buildSettingsFingerprint.get(),
                localPackageSourcesFingerprint = localPackageSourcesFingerprint(localPackageSources),
            )
        )
        // The execution key also includes the lock synchronization identifier: two lock scopes can pin different
        // resolved package versions even when their declared dependency ranges are identical.
        val xcodebuildExecutionFingerprint = normalizedXcodebuildExecutionFingerprint(
            packageResolvedSynchronization = parameters.packageResolvedSynchronization.get(),
            architectures = parameters.architectures.get(),
            additionalXcodeArgs = parameters.additionalXcodeArgs.get(),
            directSwiftPmDependencies = parameters.directSwiftPMDependencies.get(),
            normalizedTransitiveSwiftPMDependenciesInput = parameters.normalizedTransitiveSwiftPMDependenciesInput.get(),
            buildSettingsFingerprint = buildSettingsAndLocalPackagesFingerprint,
        )

        // The dump task reads this hash during its own execution and performs the build-service claim/join step.
        dumpFingerprint(xcodebuildExecutionFingerprint)
    }

    private fun dumpFingerprint(xcodebuildExecutionFingerprint: String) {
        val outputFile = parameters.xcodebuildExecutionHashFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(xcodebuildExecutionFingerprint)
    }
}

internal fun normalizedXcodebuildExecutionFingerprint(
    packageResolvedSynchronization: String,
    architectures: Set<AppleArchitecture>,
    additionalXcodeArgs: List<String>,
    directSwiftPmDependencies: Set<SwiftPMDependency>,
    normalizedTransitiveSwiftPMDependenciesInput: String,
    buildSettingsFingerprint: String,
): String {
    val payload = dumpTaskFingerprintJson.encodeToString(
        XcodebuildExecutionDumpTaskFingerprint(
            packageResolvedSynchronization = packageResolvedSynchronization,
            architectures = architectures.map { it.name }.sorted(),
            buildSettingsFingerprint = buildSettingsFingerprint,
            directSwiftPmDependencies = directSwiftPmDependencies.map { it.toDumpTaskFingerprint() }.sortedBy { it.stableSortKey },
            normalizedTransitiveSwiftPmDependenciesInput = normalizedTransitiveSwiftPMDependenciesInput,
            additionalXcodeArgs = normalizeXcodebuildArgs(additionalXcodeArgs),
        )
    )

    return MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
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

    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

internal fun normalizeXcodebuildArgs(args: List<String>): List<String> {
    // Tests can configure these args through providers in different orders. Sorting avoids splitting buckets only
    // because semantically identical optional xcodebuild arguments were declared in a different order.
    return args.sorted()
}

internal fun TransitiveSwiftPMDependencies.toNormalizedDumpTaskFingerprintInput(): String =
    dumpTaskFingerprintJson.encodeToString(normalizedTransitiveSwiftPMMetadata())

private fun TransitiveSwiftPMDependencies.normalizedTransitiveSwiftPMMetadata(): List<NormalizedTransitiveSwiftPMMetadata> =
    metadataByDependencyIdentifier.values.map { metadata ->
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
                append(metadata.dependencies.map { it.toDumpTaskFingerprint() }.sortedBy { it.stableSortKey }
                           .joinToString(";") { it.stableSortKey })
            },
            konanTargets = metadata.konanTargets.sorted(),
            iosDeploymentTarget = metadata.iosDeploymentVersion,
            macosDeploymentTarget = metadata.macosDeploymentVersion,
            watchosDeploymentTarget = metadata.watchosDeploymentVersion,
            tvosDeploymentTarget = metadata.tvosDeploymentVersion,
            dependencies = metadata.dependencies.map { it.toDumpTaskFingerprint() }.sortedBy { it.stableSortKey },
        )
    }.sortedBy { it.stableSortKey }


@Serializable
private data class XcodebuildExecutionDumpTaskFingerprint(
    val packageResolvedSynchronization: String,
    val architectures: List<String>,
    val buildSettingsFingerprint: String,
    val directSwiftPmDependencies: List<NormalizedSwiftPMDependency>,
    val normalizedTransitiveSwiftPmDependenciesInput: String,
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

private fun SwiftPMDependency.Product.toDumpTaskFingerprint(): NormalizedSwiftPMProduct = NormalizedSwiftPMProduct(
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
