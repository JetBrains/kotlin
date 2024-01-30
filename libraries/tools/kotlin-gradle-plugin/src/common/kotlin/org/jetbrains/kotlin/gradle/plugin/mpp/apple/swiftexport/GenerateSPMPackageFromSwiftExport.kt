/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow

abstract class GenerateSPMPackageFromSwiftExport : DefaultTask() {

    @get:Input
    abstract val swiftApiModuleName: Property<String>
    @get:Input
    abstract val swiftLibraryName: Property<String>
    @get:Input
    abstract val kotlinLibraryName: Property<String>

    @get:InputFile
    abstract val swiftApiPath: RegularFileProperty
    @get:InputFile
    abstract val headerBridgePath: RegularFileProperty
    @get:InputFile
    abstract val libraryPath: RegularFileProperty

    @get:OutputDirectory
    abstract val packagePath: DirectoryProperty

    @get:Internal
    val headerBridgeIncludePath get() = headerBridgeModulePath.resolve("include")

    private val swiftApiModule get() = swiftApiModuleName.get()
    private val headerBridgeModule get() = swiftApiModule + "Bridge"
    private val spmPackageRootPath get() = packagePath.get().asFile
    private val sourcesPath get() = spmPackageRootPath.resolve("Sources")
    private val swiftApiModulePath get() = sourcesPath.resolve(swiftApiModule)
    private val headerBridgeModulePath get() = sourcesPath.resolve(headerBridgeModule)

    @TaskAction
    fun generate() {
        preparePackageDirectory()
        createHeaderTarget()
        createSwiftTarget()
        createPackageManifest()
    }

    private fun preparePackageDirectory() {
        val spmSupportRootPath = packagePath.get().asFile
        if (spmSupportRootPath.exists()) {
            spmSupportRootPath.deleteRecursivelyOrThrow()
        }
        swiftApiModulePath.createDirectory()
        headerBridgeIncludePath.createDirectory()
    }

    private fun createHeaderTarget() {
        headerBridgePath.get().asFile.copyTo(
            headerBridgeIncludePath.resolve("Kotlin.h")
        )
        headerBridgeIncludePath.resolve("module.modulemap").writeText(
            """
            module $headerBridgeModule {
                umbrella "."
                export *
                
                link "${swiftLibraryName.get()}"
                link "${kotlinLibraryName.get()}"
            }
            """.trimIndent()
        )
        headerBridgeModulePath.resolve("linkingStub.c").writeText("\n")
    }

    private fun createSwiftTarget() {
        swiftApiModulePath.resolve("Kotlin.swift").writer().use { kotlinApi ->
            kotlinApi.append("import $headerBridgeModule\n\n")
            swiftApiPath.get().asFile.reader().forEachLine {
                kotlinApi.append(it).appendLine()
            }
        }
    }

    private fun createPackageManifest() {
        val manifest = spmPackageRootPath.resolve("Package.swift")
        manifest.writeText(
            """
            // swift-tools-version: 5.9
            
            import PackageDescription
            let package = Package(
                name: "$swiftApiModule",
                products: [
                    .library(
                        name: "${swiftApiModule}Library",
                        targets: ["$swiftApiModule"]
                    ),
                ],
                targets: [
                    .target(
                        name: "$swiftApiModule",
                        dependencies: ["$headerBridgeModule"]
                    ),
                    .target(
                        name: "$headerBridgeModule"
                    )
                ]
            )
            """.trimIndent()
        )
    }

}