/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleXcodeTasks
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File
import javax.inject.Inject

abstract class BuildSyntheticProjectWithSwiftExportPackage : DefaultTask() {

    @get:Inject
    abstract val providerFactory: ProviderFactory

    @get:Input
    abstract val swiftApiModuleName: Property<String>

    @get:Input
    abstract val swiftLibraryName: Property<String>

    @get:Input
    val inheritedBuildSettingsFromEnvironment: Map<String, Provider<String>> get() = listOf(
        "CONFIGURATION", "ARCHS", "ONLY_ACTIVE_ARCH",
    ).keysToMap {
        providerFactory.environmentVariable(it)
    }

    @get:Optional
    @get:Input
    val targetDeviceIdentifier: Provider<String> get() = providerFactory.environmentVariable("TARGET_DEVICE_IDENTIFIER")

    @get:Input
    val platformName: Provider<String> get() = providerFactory.environmentVariable("PLATFORM_NAME")

    @get:OutputDirectory
    val syntheticInterfacesPath: Provider<Directory> get() = syntheticProjectDirectory.map { it.dir("dd-interfaces") }

    private val syntheticObjectFilesDirectory get() = syntheticProjectDirectory.map { it.dir("dd-o-files") }

    @get:OutputFile
    val syntheticLibraryPath: Provider<RegularFile> get() = syntheticObjectFilesDirectory.map { it.file("lib${swiftLibraryName.get()}.a") }

    @get:OutputDirectory
    val syntheticBuildIntermediatesPath: Provider<File> get() = syntheticProjectDirectory.map { it.dir("dd-other") }.mapToFile()

    @get:Internal
    abstract val syntheticProjectDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val syntheticProjectPath = setUpSyntheticProject()
        val syntheticObjectFilesDirectory = buildSyntheticProject(syntheticProjectPath)
        packObjectFilesIntoLibrary(syntheticObjectFilesDirectory)
    }

    private fun setUpSyntheticProject(): File {
        val syntheticForSpm = syntheticProjectDirectory.get().asFile
        val syntheticProjectPath = syntheticForSpm.resolve("synthetic.xcodeproj")
        syntheticProjectPath.mkdirs()

        val pbxprojPath = syntheticProjectPath.resolve("project.pbxproj")
        val syntheticProjectTemplate =
            AppleXcodeTasks.javaClass.getResourceAsStream("/spm/project.pbxproj") ?: error("Missing synthetic project resource")

        pbxprojPath.outputStream().writer().use { pbxprojWriter ->
            syntheticProjectTemplate.reader().forEachLine { line ->
                pbxprojWriter
                    .append(line.replace(SYNTHETIC_PROJECT_MARKER, swiftApiModuleName.get()))
                    .appendLine()
            }
        }
        return syntheticProjectPath
    }

    private fun buildSyntheticProject(syntheticProjectPath: File): File {
        val syntheticObjectFilesDirectory = this.syntheticObjectFilesDirectory.get().asFile
        val intermediatesDestination = mapOf(
            // Thin/universal object files
            "TARGET_BUILD_DIR" to syntheticObjectFilesDirectory.canonicalPath,
            // .swiftmodule interface
            "BUILT_PRODUCTS_DIR" to syntheticInterfacesPath.get().asFile.canonicalPath
        )
        val inheritedBuildSettings = inheritedBuildSettingsFromEnvironment.mapValues {
            it.value.get()
        }

        // FIXME: This will not work with dynamic libraries
        runCommand(
            listOf(
                "xcodebuild",
                "build",
                "-derivedDataPath", syntheticBuildIntermediatesPath.get().canonicalPath,
                "-project", syntheticProjectPath.canonicalPath,
                "-scheme", swiftLibraryName.get(),
                "-destination", destination(),
            ) + (inheritedBuildSettings + intermediatesDestination).map { (k, v) -> "$k=$v" }
        )
        return syntheticObjectFilesDirectory
    }

    private fun packObjectFilesIntoLibrary(syntheticObjectFilesDirectory: File) {
        val objectFilePaths = syntheticObjectFilesDirectory.listFilesOrEmpty().filter {
            it.extension == "o"
        }.map { it.canonicalPath }
        if (objectFilePaths.isEmpty()) {
            error("Synthetic project build didn't produce any object files")
        }

        runCommand(
            listOf(
                "libtool", "-static",
                "-o", syntheticLibraryPath.get().asFile.canonicalPath,
            ) + objectFilePaths,
        )
    }

    private fun destination(): String {
        val deviceId: String? = targetDeviceIdentifier.orNull
        if (deviceId != null) return "id=$deviceId"

        val platformName = platformName.orNull ?: error("Missing a target device identifier and a platform name")
        val platform = mapOf(
            "iphonesimulator" to "iOS Simulator",
            "iphoneos" to "iOS",
            "watchsimulator" to "watchOS Simulator",
            "watchos" to "watchOS",
            "appletvos" to "tvOS",
            "appletvsimulator" to "tvOS Simulator",
            "macosx" to "macOS",
        )[platformName] ?: error("Unknown PLATFORM_NAME $platformName")

        return "generic/platform=$platform"
    }

    companion object {
        const val SYNTHETIC_PROJECT_MARKER = "<SwiftExportModuleName>"
    }

}