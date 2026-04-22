/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
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

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    private val layout = project.layout

    @get:OutputDirectory
    abstract val dumpedXcodeBuildArgsDir: DirectoryProperty

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
    @get:Internal
    abstract val additionalXcodeArgs: ListProperty<String>

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Internal
    val syntheticImportDd: Provider<Directory> =
        layout.buildDirectory.dir(XcodebuildDefFileUtils.SYNTHETIC_IMPORT_DD_DIR)

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun dumpXcodeBuildArgs() {
        if (hasSwiftPMDependencies.get()) {
            submitXcodebuildArgsDumpWorkAction()
        }
    }

    private fun submitXcodebuildArgsDumpWorkAction() {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.set(syntheticImportDd)
            params.clangDumpIntermediatesDir.set(dumpedXcodeBuildArgsDir)
            params.additionalXcodeArgs.set(additionalXcodeArgs)
        }
    }

    companion object {
        const val TASK_NAME = "dumpXcodebuildArgs"
    }
}

internal fun normalizeXcodebuildArgs(args: List<String>): List<String> {
    return args.sorted()
}

internal fun SwiftPMImportExtension.normalizedDumpTaskFingerprint(
    xcodebuildPlatform: String,
    xcodebuildSdk: String,
    architectures: Set<AppleArchitecture>,
    additionalXcodeArgs: List<String>,
): String {
    // The fingerprint serializes only build-relevant SwiftPM inputs so equal declarations map to one shared dump owner.
    val payload = dumpTaskFingerprintJson.encodeToString(
        DumpTaskFingerprint(
            xcodebuildPlatform = xcodebuildPlatform,
            xcodebuildSdk = xcodebuildSdk,
            architectures = architectures.map { it.name }.sorted(),
            iosDeploymentTarget = iosMinimumDeploymentTarget.orNull.orEmpty(),
            macosDeploymentTarget = macosMinimumDeploymentTarget.orNull.orEmpty(),
            watchosDeploymentTarget = watchosMinimumDeploymentTarget.orNull.orEmpty(),
            tvosDeploymentTarget = tvosMinimumDeploymentTarget.orNull.orEmpty(),
            swiftPmDependencies = swiftPMDependencies.map { it.toDumpTaskFingerprint() }
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
    val xcodebuildPlatform: String,
    val xcodebuildSdk: String,
    val architectures: List<String>,
    val iosDeploymentTarget: String,
    val macosDeploymentTarget: String,
    val watchosDeploymentTarget: String,
    val tvosDeploymentTarget: String,
    val swiftPmDependencies: List<NormalizedSwiftPMDependency>,
    val additionalXcodeArgs: List<String>,
)

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
