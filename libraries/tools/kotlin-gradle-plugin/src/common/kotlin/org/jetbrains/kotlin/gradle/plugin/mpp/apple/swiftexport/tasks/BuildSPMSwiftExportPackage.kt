/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportFingerprintedCoordinationService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.sharedCheckoutFor
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class BuildSPMSwiftExportPackage @Inject constructor(
    providerFactory: ProviderFactory,
    private val objectFactory: ObjectFactory,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:Input
    abstract val swiftApiModuleName: Property<String>

    @get:Input
    abstract val swiftLibraryName: Property<String>

    @get:Input
    abstract val target: Property<KonanTarget>

    @get:Input
    abstract val configuration: Property<String>

    @get:Input
    val deploymentTargetSettingName: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("DEPLOYMENT_TARGET_SETTING_NAME")
    )

    @get:Internal
    val deploymentTarget: Provider<String> = deploymentTargetSettingName.flatMap {
        providerFactory.environmentVariable(it)
    }

    @get:Optional
    @get:Input
    val targetDeviceIdentifier: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("TARGET_DEVICE_IDENTIFIER")
    )

    @get:Internal
    abstract val packageRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val packageRootTrackedFiles: FileTree
        get() = packageRoot.packageFilesWithoutSwiftPMState()

    @get:Internal
    abstract val swiftPMImportPackageRoot: DirectoryProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val swiftPMImportFingerprint: RegularFileProperty

    @get:Internal
    abstract val swiftPMImportCoordinationService: Property<SwiftImportFingerprintedCoordinationService>

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val swiftPMImportPackageTrackedFiles: FileCollection
        get() = if (swiftPMImportPackageRoot.isPresent) {
            swiftPMImportPackageRoot.packageFilesWithoutSwiftPMState()
        } else {
            objectFactory.fileCollection()
        }

    private fun DirectoryProperty.packageFilesWithoutSwiftPMState(): FileTree = asFileTree.matching {
        it.exclude("Package.resolved")
        it.exclude(".swiftpm")
        it.exclude(".build")
    }

    @get:Internal
    abstract val swiftPMImportCheckout: DirectoryProperty

    @get:OutputDirectory
    abstract val packageDerivedData: DirectoryProperty

    @get:OutputDirectory
    abstract val packageBuildDir: DirectoryProperty

    @get:OutputDirectory
    val interfacesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-interfaces"))
    }

    @get:OutputDirectory
    val objectFilesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-o-files"))
    }

    @get:OutputDirectory
    val libraryFilesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-a-files"))
    }

    @get:OutputFile
    val packageLibrary: RegularFileProperty = objectFactory.fileProperty().apply {
        set(libraryFilesPath.file(swiftLibraryName.map { "lib${it}.a" }))
    }

    private val libraryTools by lazy { LibraryTools(logger) }

    private val packageRootPath get() = packageRoot.getFile()

    @TaskAction
    fun run() {
        buildSyntheticPackage()
        packObjectFilesIntoLibrary()
    }

    private fun buildSyntheticPackage() {
        val intermediatesDestination = mapOf(
            // Thin/universal object files
            "TARGET_BUILD_DIR" to objectFilesPath.getFile().absolutePath,
            // .swiftmodule interface
            "BUILT_PRODUCTS_DIR" to interfacesPath.getFile().absolutePath,
        )

        val swiftModuleName = swiftApiModuleName.get()
        val deploymentTargetSettingName = deploymentTargetSettingName.get()
        val deploymentTarget = deploymentTarget.get()

        val buildArguments = mapOf(
            "ARCHS" to target.map { it.appleArchitecture }.get().xcodebuildArch,
            "CONFIGURATION" to configuration.get(),
            "DEPLOYMENT_TARGET_SETTING_NAME" to deploymentTargetSettingName,
            deploymentTargetSettingName to deploymentTarget,

            /*
            We need to add -public-autolink-library flag because bridge module is imported with @_implementationOnly
            All object files will be merged in `lib${swiftApiModuleName}.a`
            More information can be found here: https://github.com/swiftlang/swift/pull/35936
             */
            "OTHER_SWIFT_FLAGS" to "-Xfrontend -public-autolink-library -Xfrontend $swiftModuleName"
        )

        val derivedData = packageDerivedData.getFile()

        val effectiveCheckout = swiftPMImportFingerprint.orNull?.asFile
            ?.let { swiftPMImportCoordinationService.get().sharedCheckoutFor(it) }
            ?: swiftPMImportCheckout.orNull?.asFile
        val checkoutArguments = effectiveCheckout?.let {
            listOf(FetchSyntheticImportProjectPackages.XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, it.absolutePath)
        } ?: emptyList()

        val command = listOf(
            "xcodebuild",
            "-derivedDataPath", derivedData.relativeOrAbsolute(packageRootPath),
            "-scheme", swiftModuleName,
            "-destination", destination(),
        ) + checkoutArguments + (intermediatesDestination + buildArguments).map { (k, v) -> "$k=$v" }

        // FIXME: This will not work with dynamic libraries
        runCommand(
            command,
            logger = logger,
            processConfiguration = {
                environment().apply {
                    val exactRemovals = AppleSdk.xcodeEnvironmentDebugDylibVars + "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES"
                    keys.filter { key ->
                        key in exactRemovals || key.startsWith("OTHER_") || key.startsWith("ASSETCATALOG_")
                    }.forEach {
                        remove(it)
                    }
                }

                directory(packageRootPath)
            }
        )
    }

    private fun packObjectFilesIntoLibrary() {
        val objectFiles = objectFilesPath.asFileTree.filter {
            it.extension == "o"
        }.files.toList()

        if (objectFiles.isEmpty()) {
            error("Synthetic package build didn't produce any object files")
        }

        // When the package depends on the SwiftPM-import synthetic package, xcodebuild also drops the imported
        // packages' object files into the redirected TARGET_BUILD_DIR. Those must not be packed: the consuming app
        // links the imported packages itself (via the integrated linkage package), so packing them here would
        // duplicate their symbols in the final binary.
        val ownTargetNames = ownTargetNames()
        val objectFilePaths = if (ownTargetNames == null) objectFiles else {
            objectFiles.filter { it.nameWithoutExtension in ownTargetNames }
        }

        if (objectFilePaths.isEmpty()) {
            error(
                "None of the produced object files matched the generated package's targets $ownTargetNames. " +
                        "Produced object files: ${objectFiles.map { it.name }}"
            )
        }

        libraryTools.mergeLibraries(objectFilePaths, packageLibrary.getFile())
    }

    private fun ownTargetNames(): Set<String>? {
        if (!swiftPMImportPackageRoot.isPresent) return null
        val sources = packageRootPath.resolve(GenerateSPMPackageFromSwiftExport.SOURCES_DIRECTORY)
        val targetDirectories = sources.listFiles { file -> file.isDirectory }
            ?: error("Expected the generated package's target sources at $sources")
        return targetDirectories.map { it.name }.toSet()
    }

    private fun destination(): String {
        val deviceId = targetDeviceIdentifier.orNull
        if (deviceId != null) return "id=$deviceId"

        return target.get().appleTarget.genericPlatformDestination
    }
}
