/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.statistics.UsesBuildFusService
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import java.io.File
import java.io.Serializable
import java.security.MessageDigest
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class GenerateSyntheticLinkageImportProject : DefaultTask(), UsesBuildFusService {

    @get:Input
    protected abstract val directlyImportedDependencies: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val dependencyIdentifierToImportedSwiftPMDependencies: Property<TransitiveSwiftPMDependencies>

    @get:Internal
    val projectDirectory = project.layout.projectDirectory

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport")
    )

    @get:OutputFiles
    protected val projectRootTrackedFiles
        get() = syntheticImportProjectRoot.asFileTree.matching {
            // SwiftPM always generates Package.resolved adjacent to the Package.swift - it shouldn't invalidate this task
            it.exclude("Package.resolved")
        }

    @get:Input
    abstract val konanTargets: SetProperty<KonanTarget>

    @get:Optional
    @get:Input
    abstract val iosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val macosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val watchosDeploymentVersion: Property<String>

    @get:Optional
    @get:Input
    abstract val tvosDeploymentVersion: Property<String>

    @get:Input
    abstract val syntheticProductType: Property<SyntheticProductType>

    @get:Input
    val failOnNonIdempotentChanges: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    // This affects diagnostics because the UI in Xcode is different from KMP plugin
    @get:Input
    val buildingFromXcode: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

    enum class SyntheticProductType : Serializable {
        DYNAMIC,
        INFERRED,
    }

    @get:Inject
    protected abstract val execOps: ExecOperations

    fun configureWithExtension(swiftPMImportExtension: SwiftPMImportExtension) {
        iosDeploymentVersion.set(swiftPMImportExtension.iosMinimumDeploymentTarget)
        macosDeploymentVersion.set(swiftPMImportExtension.macosMinimumDeploymentTarget)
        watchosDeploymentVersion.set(swiftPMImportExtension.watchosMinimumDeploymentTarget)
        tvosDeploymentVersion.set(swiftPMImportExtension.tvosMinimumDeploymentTarget)
        directlyImportedDependencies.set(swiftPMImportExtension.swiftPMDependencies)
    }

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        buildFusService.orNull?.reportFusMetrics {
            it.report(
                BooleanMetrics.KMP_SWIFT_PM_IMPORT_HAS_TRANSITIVE_DEPENDENCIES_FROM_MODULAR_DEPENDENCIES,
                dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.keys.any { it.isModular }
            )
        }

        failOnNonIdempotentChangesIfNeeded {
            val packageRoot = syntheticImportProjectRoot.get().asFile.normalizedAbsoluteFile()
            when (syntheticProductType.get()) {
                SyntheticProductType.DYNAMIC -> {
                    generatePackageManifest(
                        identifier = SYNTHETIC_IMPORT_DYLIB,
                        packageRoot = packageRoot.resolve("${SUBPACKAGES}/${SYNTHETIC_IMPORT_DYLIB}"),
                        syntheticProductType = SyntheticProductType.DYNAMIC,
                        directlyImportedSwiftPMDependencies = directlyImportedDependencies.get(),
                        transitiveSyntheticPackages = dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.keys,
                        transitiveSyntheticPackagesPath = "..",
                    )
                    generatePackageManifest(
                        identifier = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                        packageRoot = packageRoot,
                        syntheticProductType = SyntheticProductType.INFERRED,
                        // Leave only version constraints - SwiftPM doesn't pick it up from subproject dependency when product is not consumed explicitly from the package
                        directlyImportedSwiftPMDependencies = directlyImportedDependencies.get().mapNotNull {
                            val remoteDependency = when (it) {
                                is SwiftPMDependency.Local -> return@mapNotNull null
                                is SwiftPMDependency.Remote -> it
                            }
                            remoteDependency.copy(products = emptyList())
                        }.toSet(),
                        transitiveSyntheticPackages = setOf(SwiftPMDependencyIdentifier(SYNTHETIC_IMPORT_DYLIB, false)),
                        transitiveSyntheticPackagesPath = SUBPACKAGES,
                    )
                }
                SyntheticProductType.INFERRED, null -> {
                    generatePackageManifest(
                        identifier = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                        packageRoot = packageRoot,
                        syntheticProductType = SyntheticProductType.INFERRED,
                        directlyImportedSwiftPMDependencies = directlyImportedDependencies.get(),
                        transitiveSyntheticPackages = dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.keys,
                        transitiveSyntheticPackagesPath = SUBPACKAGES,
                    )
                }
            }
            dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.forEach { (dependencyIdentifier, swiftPMDependencies) ->
                generatePackageManifest(
                    identifier = dependencyIdentifier.identifier,
                    packageRoot = packageRoot.resolve("${SUBPACKAGES}/${dependencyIdentifier.identifier}"),
                    /**
                     * FIXME: KT-83873 We probably always want inferred here, but figure out what is wrong with SwiftPM's linkage when 2 .dynamic products are involved
                     *
                     * Also all the project/modular dependencies will litter embedAndSign integration with useless dylibs
                     */
                    syntheticProductType = SyntheticProductType.INFERRED,
                    directlyImportedSwiftPMDependencies = swiftPMDependencies.dependencies,
                    transitiveSyntheticPackages = setOf(),
                    transitiveSyntheticPackagesPath = "..",
                )
            }
        }
    }

    private fun failOnNonIdempotentChangesIfNeeded(work: () -> Unit) {
        if (!failOnNonIdempotentChanges.get()) {
            work()
            return
        }
        val sha = MessageDigest.getInstance("SHA-256")
        val hashProjectFiles: () -> ByteArray = {
            projectRootTrackedFiles.files.sorted().forEach {
                sha.update(it.readBytes())
            }
            sha.digest()
        }

        val initialDigest = hashProjectFiles()
        work()
        val finalDigest = hashProjectFiles()

        if (!initialDigest.contentEquals(finalDigest)) {
            println("error: Synthetic project regenerated")
            if (buildingFromXcode.get()) {
                println("error: Please go to File -> Package -> Resolve Package Versions in Xcode")
            } else {
                // KMP IJ plugin
                println("error: Please go to Tools -> Swift Package Manager -> Resolve Dependencies")
            }
            error("Synthetic project state updated")
        }
    }

    private fun generatePackageManifest(
        identifier: String,
        packageRoot: File,
        syntheticProductType: SyntheticProductType,
        directlyImportedSwiftPMDependencies: Set<SwiftPMDependency>,
        transitiveSyntheticPackages: Set<SwiftPMDependencyIdentifier>,
        transitiveSyntheticPackagesPath: String,
    ) {
        val repoDependencies = (directlyImportedSwiftPMDependencies.map { importedPackage ->
            buildString {
                appendLine(".package(")
                val dependencyArguments = mutableListOf<String>()
                when (importedPackage) {
                    is SwiftPMDependency.Remote -> {
                        dependencyArguments += when (val repository = importedPackage.repository) {
                            is SwiftPMDependency.Remote.Repository.Id -> "  id: \"${repository.value}\""
                            is SwiftPMDependency.Remote.Repository.Url -> "  url: \"${repository.value}\""
                        }
                        dependencyArguments += when (val version = importedPackage.version) {
                            is SwiftPMDependency.Remote.Version.Exact -> "  exact: \"${version.value}\""
                            is SwiftPMDependency.Remote.Version.From -> "  from: \"${version.value}\""
                            is SwiftPMDependency.Remote.Version.Range -> "  \"${version.from}\"...\"${version.through}\""
                            is SwiftPMDependency.Remote.Version.Branch -> "  branch: \"${version.value}\""
                            is SwiftPMDependency.Remote.Version.Revision -> "  revision: \"${version.value}\""
                        }
                    }
                    is SwiftPMDependency.Local -> {
                        val absolutePath = importedPackage.absolutePath
                        val relativePath = absolutePath.normalizedAbsoluteFile().relativeTo(packageRoot)
                        dependencyArguments += "  path: \"${relativePath.path}\""
                    }
                }
                if (importedPackage.traits.isNotEmpty()) {
                    val traitsString = importedPackage.traits.joinToString(", ") { "\"${it}\"" }
                    dependencyArguments += "  traits: [${traitsString}]"
                }
                appendLine(dependencyArguments.joinToString(",\n"))
                append(")")
            }
        } + transitiveSyntheticPackages.map {
            ".package(path: \"${transitiveSyntheticPackagesPath}/${it.identifier}\")"
        })
        val targetDependencies = (directlyImportedSwiftPMDependencies.flatMap { dependency ->
            dependency.products.map { product -> product to dependency.packageName }
        }.map { dependency ->
            buildString {
                appendLine(".product(")
                val dependencyArguments = mutableListOf<String>()
                dependencyArguments += "  name: \"${dependency.first.name}\""
                dependencyArguments += "  package: \"${dependency.second}\""
                val platformConstraints = dependency.first.platformConstraints
                if (platformConstraints != null) {
                    val platformsString = platformConstraints.joinToString(", ") { platform -> ".${platform.swiftEnumName}" }
                    dependencyArguments += "  condition: .when(platforms: [${platformsString}])"
                }
                appendLine(dependencyArguments.joinToString(",\n"))
                append(")")
            }
        } + transitiveSyntheticPackages.map {
            ".product(name: \"${it.identifier}\", package: \"${it.identifier}\")"
        })

        val platforms = konanTargets.get().map { it.family }.toSet().map {
            when (it) {
                Family.OSX -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        macosDeploymentVersion,
                        MACOS_DEPLOYMENT_TARGET_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.values.mapNotNull { it.macosDeploymentVersion },
                    )
                    ".macOS(\"${deploymentTarget}\")"
                }
                Family.IOS -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        iosDeploymentVersion,
                        IOS_DEPLOYMENT_TARGET_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.values.mapNotNull { it.iosDeploymentVersion },
                    )
                    ".iOS(\"${deploymentTarget}\")"
                }
                Family.TVOS -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        tvosDeploymentVersion,
                        TVOS_DEPLOYMENT_TARGET_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.values.mapNotNull { it.tvosDeploymentVersion },
                    )
                    ".tvOS(\"${deploymentTarget}\")"
                }
                Family.WATCHOS -> {
                    val deploymentTarget = explicitOrMaximumDeploymentTarget(
                        watchosDeploymentVersion,
                        WATCHOS_DEPLOYMENT_TARGET_DEFAULT,
                        dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.values.mapNotNull { it.watchosDeploymentVersion },
                    )
                    ".watchOS(\"${deploymentTarget}\")"
                }
                Family.LINUX,
                Family.MINGW,
                Family.ANDROID,
                    -> error("Unexpected SwiftPM import family: $it")
            }
        }

        val productType = when (syntheticProductType) {
            SyntheticProductType.DYNAMIC -> ".dynamic"
            SyntheticProductType.INFERRED -> ".none"
        }

        val manifest = packageRoot.resolve(MANIFEST_NAME)
        manifest.also {
            it.parentFile.mkdirs()
        }.writeText(
            SwiftImportManifestGenerator.generateManifest(
                identifier = identifier,
                productType = productType,
                platforms = platforms,
                repoDependencies = repoDependencies,
                targetDependencies = targetDependencies,
            )
        )

        val objcSource = "Sources/${identifier}/${identifier}.m"
        val objcHeader = "Sources/${identifier}/include/${identifier}.h"
        // Generate ObjC sources specifically because the next CC-overriding step relies on passing a clang shim to dump compiler arguments
        packageRoot.resolve(objcSource).also {
            it.parentFile.mkdirs()
        }.writeText("")
        packageRoot.resolve(objcHeader).also {
            it.parentFile.mkdirs()
        }.writeText("")
    }

    /**
     * Calculate the deployment target:
     * - Use an explicit version from the build script if set
     * - Otherwise take the maximum version of all transitive packages (otherwise the package will not build)
     *
     * We also use a [deploymentVersionDefault] as the minimum implicit version because SwiftPM by default uses an ancient deployment default
     */
    private fun explicitOrMaximumDeploymentTarget(
        deploymentVersionProperty: Provider<String>,
        deploymentVersionDefault: String,
        transitivelyImportedDeploymentVersions: List<String>,
    ): String {
        val explicitlySpecifiedDeploymentVersion = deploymentVersionProperty.orNull
        if (explicitlySpecifiedDeploymentVersion != null) {
            return explicitlySpecifiedDeploymentVersion
        }
        val maximumDeploymentTarget = transitivelyImportedDeploymentVersions.fold(
            SemVer.from(deploymentVersionDefault, loose = true),
        ) { max, current ->
            val other = SemVer.from(current, loose = true)
            if (max >= other) {
                max
            } else {
                other
            }
        }
        return "${maximumDeploymentTarget.major}.${maximumDeploymentTarget.minor}"
    }

    companion object {
        const val TASK_NAME = "generateSyntheticLinkageSwiftPMImportProject"
        const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME = "KotlinMultiplatformLinkedPackage"
        const val SYNTHETIC_IMPORT_DYLIB = "KotlinMultiplatformLinkedPackageDylib"
        const val SUBPACKAGES = "subpackages"
        const val MANIFEST_NAME = "Package.swift"

        val syntheticImportProjectGenerationTaskName = lowerCamelCaseName(
            TASK_NAME,
            "forCinteropsAndLdDump",
        )

        const val IOS_DEPLOYMENT_TARGET_DEFAULT = "15.0"
        const val MACOS_DEPLOYMENT_TARGET_DEFAULT = "10.15"
        const val WATCHOS_DEPLOYMENT_TARGET_DEFAULT = "15.0"
        const val TVOS_DEPLOYMENT_TARGET_DEFAULT = "9.0"
    }
}
