/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.LibraryTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class BuildSPMSwiftExportPackage @Inject constructor(
    providerFactory: ProviderFactory,
    objectsFactory: ObjectFactory,
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

    @get:Optional
    @get:Input
    val targetDeviceIdentifier: Property<String> = objectsFactory.property<String>().convention(
        providerFactory.environmentVariable("TARGET_DEVICE_IDENTIFIER")
    )

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val packageDerivedData: DirectoryProperty

    @get:OutputDirectory
    abstract val packageBuildDir: DirectoryProperty

    @get:OutputDirectory
    val interfacesPath: DirectoryProperty = objectsFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-interfaces"))
    }

    @get:OutputDirectory
    val objectFilesPath: DirectoryProperty = objectsFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-o-files"))
    }

    @get:OutputDirectory
    val libraryFilesPath: DirectoryProperty = objectsFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-a-files"))
    }

    @get:OutputFile
    val packageLibrary: RegularFileProperty = objectsFactory.fileProperty().apply {
        set(libraryFilesPath.file(swiftLibraryName.map { "lib${it}.a" }))
    }

    private val libraryTools by lazy { LibraryTools(logger) }

    private val packageRootPath get() = packageRoot.getFile()
    private val architecture: Provider<String> by lazy { target.map { it.appleArchitecture() } }
    private val platform: Provider<String> by lazy { target.map { it.applePlatform() } }

    @TaskAction
    fun run() {
        buildSyntheticPackage()
        packObjectFilesIntoLibrary()
    }

    private fun buildSyntheticPackage() {
        val intermediatesDestination = mapOf(
            // Thin/universal object files
            "TARGET_BUILD_DIR" to objectFilesPath.getFile().canonicalPath,
            // .swiftmodule interface
            "BUILT_PRODUCTS_DIR" to interfacesPath.getFile().canonicalPath,
        )

        val buildArguments = mapOf(
            "ARCHS" to architecture.get(),
            "CONFIGURATION" to configuration.get(),
        )

        val derivedData = packageDerivedData.getFile()
        val scheme = swiftApiModuleName.get()

        val command = listOf(
            "xcodebuild",
            "-derivedDataPath", derivedData.relativeOrAbsolute(packageRootPath),
            "-scheme", scheme,
            "-destination", destination(),
        ) + (intermediatesDestination + buildArguments).map { (k, v) -> "$k=$v" }

        // FIXME: This will not work with dynamic libraries
        runCommand(
            command,
            logger = logger,
            processConfiguration = {
                directory(packageRootPath)
            }
        )
    }

    private fun packObjectFilesIntoLibrary() {
        val objectFilePaths = objectFilesPath.asFileTree.filter {
            it.extension == "o"
        }.files.toList()

        if (objectFilePaths.isEmpty()) {
            error("Synthetic package build didn't produce any object files")
        }

        libraryTools.mergeLibraries(objectFilePaths, packageLibrary.getFile())
    }

    private fun destination(): String {
        val deviceId = targetDeviceIdentifier.orNull
        if (deviceId != null) return "id=$deviceId"

        val platformName = platform.orNull ?: error("Missing platform name")
        return "generic/platform=$platformName"
    }
}