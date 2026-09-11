/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.ModuleMapGenerator
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportFingerprintedCoordinationService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SyntheticPackageChangeReport
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.sharedPackageRootFor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.utils.CommaSeparatedEntriesBuilder
import org.jetbrains.kotlin.gradle.utils.StringBlockBuilder
import org.jetbrains.kotlin.gradle.utils.buildStringBlock
import org.jetbrains.kotlin.gradle.utils.commaSeparatedEntries
import org.jetbrains.kotlin.gradle.utils.emitListItems
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

    /**
     * The name of the SwiftPM binary target that carries the Kotlin binaries. When set, the manifest declares
     * `<name>.xcframework` at the package root and makes the Kotlin runtime target depend on it.
     */
    @get:Optional
    @get:Input
    abstract val kotlinBinaryTargetName: Property<String>

    /**
     * The `platforms:` the generated manifest declares: SwiftPM platform name (`iOS`, `macOS`, ...) to the
     * minimum version the exported Swift requires. Empty means no `platforms:` entry, which leaves the
     * deployment target to whoever builds the package.
     */
    @get:Input
    abstract val platforms: MapProperty<String, String>

    /**
     * The Swift Export output the package sources are taken from. [swiftModulesFile] only names the files, so
     * this has to be an input of its own: otherwise a change to the generated `.swift` or `.h` content alone
     * would leave this task UP-TO-DATE with stale package sources.
     *
     * Also compared with [otherSwiftExportOutputs] to report targets whose output differs from it. Optional
     * because the Xcode flow never sets it.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val primarySwiftExportOutput: DirectoryProperty

    /**
     * Swift Export outputs of the other targets that share this package. Each is compared with
     * [primarySwiftExportOutput]; differences are reported as a warning because the package carries one copy of
     * the sources.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val otherSwiftExportOutputs: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinRuntime: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftModulesFile: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val swiftPMImportProductName: Property<String>

    @get:Internal
    abstract val swiftPMImportPackageRoot: DirectoryProperty

    @get:Optional
    @get:Input
    protected val swiftPMImportPackageRootPath: Provider<String>
        get() = swiftPMImportPackageRoot.locationOnly.map { it.asFile.absolutePath }

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val swiftPMImportFingerprint: RegularFileProperty

    @get:Internal
    abstract val swiftPMImportCoordinationService: Property<SwiftImportFingerprintedCoordinationService>

    @get:OutputDirectory
    abstract val packagePath: DirectoryProperty

    @get:OutputDirectory
    val includesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packagePath.dir("OtherIncludes"))
    }

    @get:OutputDirectory
    val sourcesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packagePath.dir(SOURCES_DIRECTORY))
    }

    private val swiftLibrary get() = swiftLibraryName.get()
    private val swiftApiModule get() = swiftApiModuleName.get()
    private val kotlinRuntimeModule get() = kotlinRuntime.getFile().name.split('_').joinToString(separator = "") { it.capitalizeAsciiOnly() }

    @TaskAction
    fun generate() {
        val swiftModules = deserializeSwiftModules()

        reportDivergentSwiftExportOutputs()
        cleanGeneratedPackageDirectories(sourcesPath.getFile(), includesPath.getFile())
        createSPMSources(swiftModules)
        createPackageManifest(swiftModules)
        createKotlinRuntimeTarget()
        createPackageMarker()
    }

    /**
     * Marks the generated package so that the export Sync carries the marker into its destination, which is
     * how `SwiftPackageOutputDirectoryChecker` tells a previously exported directory from one the user filled
     * with something else.
     */
    private fun createPackageMarker() {
        packagePath.getFile()
            .resolve(SwiftExportConstants.SWIFT_PACKAGE_MARKER_FILE_NAME)
            .writeText(SwiftExportConstants.SWIFT_PACKAGE_MARKER_FILE_CONTENT)
    }

    private fun reportDivergentSwiftExportOutputs() {
        val primary = primarySwiftExportOutput.orNull?.asFile ?: return
        otherSwiftExportOutputs.files.forEach { other ->
            val changes = SyntheticPackageChangeReport.diff(snapshotDirectory(primary), snapshotDirectory(other))
            if (!changes.isEmpty) {
                logger.warn(renderSwiftExportOutputDivergence(primary, other, changes))
            }
        }
    }

    private fun deserializeSwiftModules(): List<GradleSwiftExportModule> {
        val modulesFile = swiftModulesFile.getFile().readText()
        val swiftModules = SerializationTools.readFromJson(modulesFile)
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
        val cinteropImport = if (swiftPMImportProductName.isPresent && swiftPMImportPackageRoot.isPresent) {
            val root = swiftPMImportFingerprint.orNull?.asFile
                ?.let { swiftPMImportCoordinationService.get().sharedPackageRootFor(it) }
                ?: swiftPMImportPackageRoot.getFile()
            CinteropPackageImport(
                path = root.absolutePath,
                productName = swiftPMImportProductName.get(),
                packageIdentity = root.name,
            )
        } else null
        val content = SPMManifestGenerator.generateManifest(
            swiftApiModule, swiftLibrary, kotlinRuntimeModule, modules, cinteropImport, kotlinBinaryTargetName.orNull,
            platforms.get()
        )
        manifest.writeText(content)
    }

    private fun appendToOtherIncludes(name: String, path: File) {
        val includesPath = includesPath.get()
        fileSystem.copy {
            it.from(path)
            it.into(includesPath.dir(name))
        }
    }

    companion object {
        const val SOURCES_DIRECTORY = "Sources"
    }
}

/**
 * Empties the directories [GenerateSPMPackageFromSwiftExport] regenerates from scratch.
 *
 * The task writes every file it owns on every run but never removes one, so a module that was renamed or
 * dropped left its `Sources/<Old>` directory behind, and the export Sync then copied it into the user's
 * package. Follows `GenerateSyntheticLinkageImportProject.removeStaleSubpackages`, in its simplest correct
 * form: the whole content is rewritten, so the whole content can go first.
 */
internal fun cleanGeneratedPackageDirectories(sources: File, includes: File) {
    listOf(sources, includes).forEach { directory ->
        directory.deleteRecursively()
        directory.mkdirs()
    }
}

internal data class CinteropPackageImport(
    val path: String,
    val productName: String,
    val packageIdentity: String,
) {
    fun productExpression(): String = ".product(name: \"$productName\", package: \"$packageIdentity\")"
}

internal object SPMManifestGenerator {

    fun generateManifest(
        swiftApiModule: String,
        swiftLibrary: String,
        kotlinRuntime: String,
        modules: List<GradleSwiftExportModule>,
        cinteropImport: CinteropPackageImport? = null,
        kotlinBinaryTarget: String? = null,
        platforms: Map<String, String> = emptyMap(),
    ): String = buildStringBlock {
        line("// swift-tools-version: 5.9")
        line()
        line("import PackageDescription")
        block("let package = Package(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$swiftApiModule\"") }
                if (platforms.isNotEmpty()) {
                    entry {
                        block("platforms: [", "]") {
                            emitListItems(platforms.map { (name, version) -> ".$name(\"$version\")" })
                        }
                    }
                }
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
                if (cinteropImport != null) {
                    entry {
                        block("dependencies: [", "]") {
                            line(".package(path: \"${cinteropImport.path}\")")
                        }
                    }
                }
                entry {
                    block("targets: [", "]") {
                        commaSeparatedEntries {
                            if (kotlinBinaryTarget != null) {
                                entry { emitBinaryTarget(kotlinBinaryTarget) }
                            }
                            emitTargetDefinitions(modules, kotlinRuntime, cinteropImport?.productExpression())
                            entry { emitTarget(kotlinRuntime, dependencies = listOfNotNull(kotlinBinaryTarget)) }
                        }
                    }
                }
            }
        }
    }

    private fun StringBlockBuilder.emitBinaryTarget(name: String) {
        block(".binaryTarget(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$name\"") }
                entry { line("path: \"$name.xcframework\"") }
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
        rawDependencies: List<String> = emptyList(),
    ) {
        block(".target(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$name\"") }
                val deps = (dependencies?.map { "\"$it\"" } ?: emptyList()) + rawDependencies
                if (deps.isNotEmpty()) {
                    entry { line("dependencies: [${deps.joinToString(", ")}]") }
                }
            }
        }
    }

    private fun CommaSeparatedEntriesBuilder.emitTargetDefinitions(
        modules: List<GradleSwiftExportModule>,
        kotlinRuntime: String,
        cinteropProductExpression: String?,
    ) {
        // The reexported cinterop's `import`s live in the Swift API targets, so each gets the product dependency.
        val rawDependencies = listOfNotNull(cinteropProductExpression)
        modules.forEach { module ->
            entry { emitTarget(module.name, module.spmDependencies(kotlinRuntime), rawDependencies) }
            if (module is GradleSwiftExportModule.BridgesToKotlin) {
                entry { emitTarget(module.bridgeName) }
            }
        }
    }
}
