/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class GenerateSPMPackageFromSwiftExport : DefaultTask() {
    companion object {
        private const val KOTLIN_RUNTIME = "KotlinRuntime"
    }

    @get:Input
    abstract val swiftApiModuleName: Property<String>

    @get:Input
    abstract val headerBridgeModuleName: Property<String>

    @get:Input
    abstract val swiftLibraryName: Property<String>

    @get:Input
    abstract val kotlinLibraryName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinRuntime: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftApiPath: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headerBridgePath: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraryPath: RegularFileProperty

    @get:OutputDirectory
    abstract val packagePath: DirectoryProperty

    @get:Internal
    val headerBridgeIncludePath get() = headerBridgeModulePath.resolve("include")

    @get:Internal
    val kotlinRuntimeIncludePath get() = kotlinRuntimeModulePath.resolve("include")

    private val swiftApiModule get() = swiftApiModuleName.get()
    private val headerBridgeModule get() = headerBridgeModuleName.get()
    private val spmPackageRootPath get() = packagePath.getFile()

    private val sourcesPath get() = spmPackageRootPath.resolve("Sources")
    private val swiftApiModulePath get() = sourcesPath.resolve(swiftApiModule)
    private val headerBridgeModulePath get() = sourcesPath.resolve(headerBridgeModule)
    private val kotlinRuntimeModulePath get() = sourcesPath.resolve(KOTLIN_RUNTIME)

    @TaskAction
    fun generate() {
        preparePackageDirectory()
        createHeaderTarget()
        createSwiftTarget()
        createPackageManifest()
        createKotlinRuntimeTarget()
    }

    private fun preparePackageDirectory() {
        if (spmPackageRootPath.exists()) {
            spmPackageRootPath.deleteRecursivelyOrThrow()
        }
        swiftApiModulePath.createDirectory()
        headerBridgeIncludePath.createDirectory()
        kotlinRuntimeIncludePath.createDirectory()
    }

    private fun createHeaderTarget() {
        headerBridgePath.getFile().copyTo(
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

    private fun createKotlinRuntimeTarget() {
        kotlinRuntime.asFileTree.forEach {
            it.copyTo(
                kotlinRuntimeIncludePath.resolve(it.name)
            )
        }
        kotlinRuntimeModulePath.resolve("linkingStub.c").writeText("\n")
    }

    private fun createSwiftTarget() {
        swiftApiModulePath.resolve("Kotlin.swift").writer().use { kotlinApi ->
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
                        dependencies: ["$headerBridgeModule", "$KOTLIN_RUNTIME"]
                    ),
                    .target(
                        name: "$headerBridgeModule"
                    ),
                    .target(
                        name: "$KOTLIN_RUNTIME"
                    )
                ]
            )
            """.trimIndent()
        )
    }

}