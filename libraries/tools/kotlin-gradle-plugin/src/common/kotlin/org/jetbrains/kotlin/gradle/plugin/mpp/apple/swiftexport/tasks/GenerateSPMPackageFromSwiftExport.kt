/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.ModuleMapGenerator
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModules
import org.jetbrains.kotlin.gradle.utils.CommaSeparatedEntriesBuilder
import org.jetbrains.kotlin.gradle.utils.StringBlockBuilder
import org.jetbrains.kotlin.gradle.utils.buildStringBlock
import org.jetbrains.kotlin.gradle.utils.commaSeparatedEntries
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class GenerateSPMPackageFromSwiftExport @Inject constructor(
    objectFactory: ObjectFactory,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:Input
    abstract val swiftApiModuleName: Property<String>

    @get:Input
    abstract val swiftLibraryName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinRuntime: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftModulesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val packagePath: DirectoryProperty

    @get:OutputDirectory
    val includesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packagePath.dir("OtherIncludes"))
    }

    @get:OutputDirectory
    val sourcesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packagePath.dir("Sources"))
    }

    private val swiftLibrary get() = swiftLibraryName.get()
    private val swiftApiModule get() = swiftApiModuleName.get()
    private val kotlinRuntimeModule get() = kotlinRuntime.getFile().name.split('_').joinToString(separator = "") { it.capitalizeAsciiOnly() }

    @TaskAction
    fun generate() {
        val swiftModules = deserializeSwiftModules()

        createSPMSources(swiftModules)
        createPackageManifest(swiftModules)
        createKotlinRuntimeTarget()
    }

    private fun deserializeSwiftModules(): List<GradleSwiftExportModule> {
        val modulesFile = swiftModulesFile.getFile().readText()
        val swiftModules = SerializationTools.readFromJson<GradleSwiftExportModules>(modulesFile)
        return swiftModules.modules
    }

    private fun createSPMSources(modules: List<GradleSwiftExportModule>) {
        modules.forEach { module ->

            fun createSwiftApi(swiftApi: File) {
                val swiftModulePath = sourcesPath.getFile().resolve(module.name).apply { createDirectory() }

                fileSystem.copy {
                    it.from(swiftApi)
                    it.into(swiftModulePath)
                }
            }

            when (module) {
                is GradleSwiftExportModule.BridgesToKotlin -> {
                    createSwiftApi(module.files.swiftApi)

                    val bridgeModulePath = sourcesPath.getFile().resolve(module.bridgeName).apply { createDirectory() }
                    val includePath = bridgeModulePath.resolve("include")

                    fileSystem.copy {
                        it.from(module.files.cHeaderBridges)
                        it.into(includePath)
                    }

                    createModuleMap(includePath, module.bridgeName, module.name)
                    bridgeModulePath.resolve("linkingStub.c").writeText("\n")

                    appendToOtherIncludes(module.bridgeName, includePath)
                }
                is GradleSwiftExportModule.SwiftOnly -> {
                    createSwiftApi(module.swiftApi)
                }
            }
        }
    }

    private fun createModuleMap(modulePath: File, moduleName: String, linkModule: String) {
        modulePath.resolve("module.modulemap").writeText(
            ModuleMapGenerator.generateModuleMap {
                name = moduleName
                export = "*"
                umbrella = "."
                link = listOf(linkModule)
            }
        )
    }

    private fun createKotlinRuntimeTarget() {
        val kotlinRuntimeModulePath = sourcesPath.getFile().resolve(kotlinRuntimeModule)
        val kotlinRuntimeIncludePath = kotlinRuntimeModulePath.resolve("include")

        fileSystem.copy {
            it.from(kotlinRuntime)
            it.into(kotlinRuntimeIncludePath)
        }

        kotlinRuntimeModulePath.resolve("linkingStub.c").writeText("\n")
        appendToOtherIncludes(kotlinRuntimeModule, kotlinRuntimeIncludePath)
    }

    private fun createPackageManifest(modules: List<GradleSwiftExportModule>) {
        val manifest = packagePath.getFile().resolve("Package.swift")
        val content = SPMManifestGenerator.generateManifest(swiftApiModule, swiftLibrary, kotlinRuntimeModule, modules)
        manifest.writeText(content)
    }

    private fun appendToOtherIncludes(name: String, path: File) {
        val includesPath = includesPath.get()
        fileSystem.copy {
            it.from(path)
            it.into(includesPath.dir(name))
        }
    }
}

internal object SPMManifestGenerator {

    fun generateManifest(
        swiftApiModule: String,
        swiftLibrary: String,
        kotlinRuntime: String,
        modules: List<GradleSwiftExportModule>,
    ): String = buildStringBlock {
        line("// swift-tools-version: 5.9")
        line()
        line("import PackageDescription")
        block("let package = Package(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$swiftApiModule\"") }
                entry {
                    block("products: [", "]") {
                        block(".library(", ")") {
                            commaSeparatedEntries {
                                entry { line("name: \"$swiftLibrary\"") }
                                entry { line("targets: [${modules.productTargets().joinToString(", ")}]") }
                            }
                        }
                    }
                }
                entry {
                    block("targets: [", "]") {
                        commaSeparatedEntries {
                            emitTargetDefinitions(modules, kotlinRuntime)
                            entry { emitTarget(kotlinRuntime) }
                        }
                    }
                }
            }
        }
    }

    private fun GradleSwiftExportModule.spmDependencies(kotlinRuntime: String): List<String> {
        return when (this) {
            is GradleSwiftExportModule.BridgesToKotlin -> dependencies + listOf(bridgeName, kotlinRuntime)
            is GradleSwiftExportModule.SwiftOnly -> dependencies + listOf(kotlinRuntime)
        }
    }

    private fun List<GradleSwiftExportModule>.productTargets(): List<String> {
        return this.map { "\"${it.name}\"" }
    }

    private fun StringBlockBuilder.emitTarget(
        name: String,
        dependencies: List<String>? = null,
    ) {
        block(".target(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$name\"") }
                if (dependencies != null) {
                    entry { line("dependencies: [${dependencies.joinToString(", ") { "\"$it\"" }}]") }
                }
            }
        }
    }

    private fun CommaSeparatedEntriesBuilder.emitTargetDefinitions(
        modules: List<GradleSwiftExportModule>,
        kotlinRuntime: String,
    ) {
        modules.forEach { module ->
            entry { emitTarget(module.name, module.spmDependencies(kotlinRuntime)) }
            if (module is GradleSwiftExportModule.BridgesToKotlin) {
                entry { emitTarget(module.bridgeName) }
            }
        }
    }
}
