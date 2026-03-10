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
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.io.Serializable
import java.security.MessageDigest
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class GenerateSyntheticLinkageImportProject : DefaultTask() {

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
        failOnNonIdempotentChangesIfNeeded {
            val packageRoot = syntheticImportProjectRoot.get().asFile.normalizedAbsoluteFile()
            val linkerHack = when (syntheticProductType.get()) {
                SyntheticProductType.DYNAMIC -> packageRoot.resolve("linkerHack").also {
                    it.writeText(linkerScriptHack())
                    it.setExecutable(true)
                }
                SyntheticProductType.INFERRED -> null
                null -> null
            }
            generatePackageManifest(
                identifier = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                packageRoot = packageRoot,
                syntheticProductType = syntheticProductType.get(),
                directlyImportedSwiftPMDependencies = directlyImportedDependencies.get(),
                transitiveSyntheticPackages = dependencyIdentifierToImportedSwiftPMDependencies.get().metadataByDependencyIdentifier.keys,
                linkerHackPath = linkerHack,
            )
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
        linkerHackPath: File? = null,
    ) {
        val repoDependencies = (directlyImportedSwiftPMDependencies.map { importedPackage ->
            buildString {
                appendLine(".package(")
                when (importedPackage) {
                    is SwiftPMDependency.Remote -> {
                        when (val repository = importedPackage.repository) {
                            is SwiftPMDependency.Remote.Repository.Id -> {
                                appendLine("  id: \"${repository.value}\",")
                            }
                            is SwiftPMDependency.Remote.Repository.Url -> {
                                appendLine("  url: \"${repository.value}\",")
                            }
                        }
                        when (val version = importedPackage.version) {
                            is SwiftPMDependency.Remote.Version.Exact -> appendLine("  exact: \"${version.value}\",")
                            is SwiftPMDependency.Remote.Version.From -> appendLine("  from: \"${version.value}\",")
                            is SwiftPMDependency.Remote.Version.Range -> appendLine("  \"${version.from}\"...\"${version.through}\",")
                            is SwiftPMDependency.Remote.Version.Branch -> appendLine("  branch: \"${version.value}\",")
                            is SwiftPMDependency.Remote.Version.Revision -> appendLine("  revision: \"${version.value}\",")
                        }
                    }
                    is SwiftPMDependency.Local -> {
                        val absolutePath = importedPackage.absolutePath
                        val relativePath = absolutePath.normalizedAbsoluteFile().relativeTo(packageRoot)
                        appendLine("  path: \"${relativePath.path}\",")
                    }
                }
                if (importedPackage.traits.isNotEmpty()) {
                    val traitsString = importedPackage.traits.joinToString(", ") { "\"${it}\"" }
                    appendLine("  traits: [${traitsString}],")
                }
                append(")")
            }
        } + transitiveSyntheticPackages.map {
            ".package(path: \"${SUBPACKAGES}/${it.identifier}\")"
        })
        val targetDependencies = (directlyImportedSwiftPMDependencies.flatMap { dep -> dep.products.map { it to dep.packageName } }.map {
            buildString {
                appendLine(".product(")
                appendLine("  name: \"${it.first.name}\",")
                appendLine("  package: \"${it.second}\",")
                val platformConstraints = it.first.platformConstraints
                if (platformConstraints != null) {
                    val platformsString = platformConstraints.joinToString(", ") { ".${it.swiftEnumName}" }
                    appendLine("  condition: .when(platforms: [${platformsString}]),")
                }
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
                linkerHackPath = linkerHackPath?.path,
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

    private fun linkerScriptHack(): String = """
    #!/usr/bin/python3
    import os
    import sys
    from os.path import dirname
    
    if __name__ == '__main__':
        is_synthetic_linkage_call = False
        for arg in sys.argv:
            if arg.startswith('@rpath') and '${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}' in arg:
                is_synthetic_linkage_call = True
    
        if is_synthetic_linkage_call:
            print(sys.argv)
            filelist_index = None
            platform_version = None
            arch = None
            output = None
            syslibroot = None
            dependency_info = None
            install_name = None
    
            for (index, arg) in enumerate(sys.argv[1:]):
                if arg == '-platform_version':
                    platform_version = sys.argv[index + 2:index + 5]
                if arg == '-filelist':
                    filelist_index = index + 2
    
                if arg == '-arch':
                    arch = sys.argv[index + 2]
                if arg == '-o':
                    output = sys.argv[index + 2]
                if arg == '-syslibroot':
                    syslibroot = sys.argv[index + 2]
                if arg == '-dependency_info':
                    dependency_info = sys.argv[index + 2]
                if arg == '-install_name' or arg == '-dylib_install_name':
                    install_name = sys.argv[index + 2]
            if filelist_index is None:
                raise 'No filelist'
    
            filelist_path = sys.argv[filelist_index]
            empty_object_file = None
            with open(filelist_path, 'r') as file:
                for line in file:
                    if '${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.o' in line:
                        empty_object_file = line
            if empty_object_file is None:
                raise f'Missing empty object file {filelist_path}'
    
            new_filelist = os.path.join(dirname(filelist_path), '_kotlinSwiftPMImport')
            with open(new_filelist, 'w') as file:
                file.write(empty_object_file)
    
            stub_ld_call = [
                "-dylib",
                "-dynamic",
                "-filelist", new_filelist,
                "-arch", arch,
                "-platform_version", *platform_version,
                "-syslibroot", syslibroot,
                "-dependency_info", dependency_info,
                "-install_name", install_name,
                "-lSystem",
                "-export_dynamic",
                "-o", output,
            ]
            print('ld ' + ' '.join([f'\'{arg}\'' for arg in stub_ld_call]))
            print(stub_ld_call)
            print(sys.argv)
            os.execlp('ld', 'ld', *stub_ld_call)
        else:
            os.execlp('ld', 'ld', *sys.argv[1:])

""".trimIndent()

    companion object {
        const val TASK_NAME = "generateSyntheticLinkageSwiftPMImportProject"
        const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME = "KotlinMultiplatformLinkedPackage"
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