/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.ModuleMapGenerator
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportFingerprintedCoordinationService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.sharedPackageRootFor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftPackagePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.swiftPackagePlatformNames
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.swiftModulesFile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.CommaSeparatedEntriesBuilder
import org.jetbrains.kotlin.gradle.utils.StringBlockBuilder
import org.jetbrains.kotlin.gradle.utils.buildStringBlock
import org.jetbrains.kotlin.gradle.utils.commaSeparatedEntries
import org.jetbrains.kotlin.gradle.utils.emitListItems
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.io.File
import javax.inject.Inject

/**
 * The output of Swift Export for one Kotlin target.
 */
internal abstract class SwiftExportTargetOutput {
    /** The name the target has in the build script. */
    @get:Input
    abstract val targetName: Property<String>

    @get:Input
    abstract val target: Property<KonanTarget>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftModulesFile: RegularFileProperty

    /** The generated files. [swiftModulesFile] has their paths, but not their content. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection
}

internal fun ObjectFactory.SwiftExportTargetOutput(
    target: KotlinNativeTarget,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): SwiftExportTargetOutput = newInstance<SwiftExportTargetOutput>().apply {
    targetName.set(target.name)
    this.target.set(target.konanTarget)
    swiftModulesFile.set(swiftExportTask.flatMap { it.parameters.swiftModulesFile })
    files.from(swiftExportTask.flatMap { it.parameters.outputDirectory })
}

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class GenerateSPMPackageFromSwiftExport @Inject constructor(
    objectFactory: ObjectFactory,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
        collectIncludes.convention(false)
    }

    @get:Input
    abstract val swiftApiModuleName: Property<String>

    @get:Input
    abstract val swiftLibraryName: Property<String>

    /**
     * The name of the binary target for `<name>.xcframework` at the root of the package. Not set in the Xcode
     * flow, where the package has no binary target.
     */
    @get:Optional
    @get:Input
    abstract val kotlinBinaryTargetName: Property<String>

    /** The `platforms:` of the manifest. */
    @get:Input
    abstract val platforms: ListProperty<SwiftPackagePlatform>

    /**
     * The targets whose sources are combined in the package, see [combineSwiftExportSources]. Empty in the
     * Xcode flow, which generates the package of one target from [swiftModulesFile].
     */
    @get:Nested
    abstract val targetOutputs: ListProperty<SwiftExportTargetOutput>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinRuntime: DirectoryProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftModulesFile: RegularFileProperty

    @get:Input
    val swiftPMImportHasDependencies: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

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

    /**
     * Whether to collect the headers of every module into [includesPath] for the Xcode integration, which copies
     * them into Xcode's products directory. Off in the package flow: the package carries its headers in its targets.
     */
    @get:Input
    abstract val collectIncludes: Property<Boolean>

    /** Inside [packagePath], so it needs no output declaration of its own. */
    @get:Internal
    val includesPath: Provider<Directory>
        get() = packagePath.dir("OtherIncludes")

    @get:OutputDirectory
    val sourcesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packagePath.dir(SOURCES_DIRECTORY))
    }

    private val swiftLibrary get() = swiftLibraryName.get()
    private val swiftApiModule get() = swiftApiModuleName.get()
    private val kotlinRuntimeModule get() = kotlinRuntime.getFile().name.split('_').joinToString(separator = "") { it.capitalizeAsciiOnly() }

    @TaskAction
    fun generate() {
        val targetModules = deserializeTargetModules()
        val packageModules = if (targetModules.isEmpty()) {
            deserializeSwiftModules().map { SwiftPackageModule(it, variants = emptyMap(), dependencyPlatforms = emptyMap()) }
        } else {
            combineSwiftExportModules(targetModules)
        }

        // Gradle keeps what a previous run left in an output directory, such as the sources of a renamed module.
        fileSystem.delete { it.delete(sourcesPath, includesPath) }
        createSPMSources(packageModules, packageTargets = targetModules.map { it.target })
        createPackageManifest(packageModules)
        createKotlinRuntimeTarget()
    }

    private fun deserializeSwiftModules(): List<GradleSwiftExportModule> {
        check(swiftModulesFile.isPresent) { "Neither targetOutputs nor swiftModulesFile is set for $path" }
        val modulesFile = swiftModulesFile.getFile().readText()
        val swiftModules = SerializationTools.readFromJson(modulesFile)
        return swiftModules.modules
    }

    private fun deserializeTargetModules(): List<SwiftExportTargetModules> = targetOutputs.get().map { output ->
        SwiftExportTargetModules(
            output.targetName.get(),
            output.target.get(),
            SerializationTools.readFromJson(output.swiftModulesFile.getFile().readText()).modules,
        )
    }

    /**
     * Writes the [source] file of [packageModule] into [destination], combined from its variants if there are any.
     */
    private fun createSource(
        packageModule: SwiftPackageModule,
        packageTargets: List<KonanTarget>,
        destination: File,
        language: SwiftPackageSourceLanguage,
        source: (GradleSwiftExportModule) -> File,
    ) {
        val module = packageModule.module
        if (packageModule.variants.isEmpty()) {
            fileSystem.copy {
                it.from(source(module))
                it.into(destination)
            }
            return
        }

        val variants = packageModule.variants.map { (target, variant) -> SwiftExportSourceVariant(target, source(variant).readText()) }
        destination.createDirectory()
        destination.resolve(source(module).name).writeText(combineSwiftExportSources(variants, language, packageTargets))
    }

    private fun createSPMSources(packageModules: List<SwiftPackageModule>, packageTargets: List<KonanTarget>) {
        packageModules.forEach { packageModule ->
            val module = packageModule.module
            val swiftModulePath = sourcesPath.getFile().resolve(module.name).apply { createDirectory() }
            createSource(packageModule, packageTargets, swiftModulePath, SwiftPackageSourceLanguage.SWIFT) { it.swiftApiFile }

            if (module is GradleSwiftExportModule.BridgesToKotlin) {
                val bridgeModulePath = sourcesPath.getFile().resolve(module.bridgeName).apply { createDirectory() }
                val includePath = bridgeModulePath.resolve("include")

                createSource(packageModule, packageTargets, includePath, SwiftPackageSourceLanguage.C_HEADER) { it.bridgeHeaderFile }

                createModuleMap(includePath, module.bridgeName, module.name)
                bridgeModulePath.resolve("linkingStub.c").writeText("\n")

                appendToOtherIncludes(module.bridgeName, includePath)
            }
        }
    }

    private val GradleSwiftExportModule.swiftApiFile: File
        get() = when (this) {
            is GradleSwiftExportModule.BridgesToKotlin -> files.swiftApi
            is GradleSwiftExportModule.SwiftOnly -> swiftApi
        }

    private val GradleSwiftExportModule.bridgeHeaderFile: File
        get() = when (this) {
            is GradleSwiftExportModule.BridgesToKotlin -> files.cHeaderBridges
            is GradleSwiftExportModule.SwiftOnly -> error("Swift module '$name' doesn't bridge to Kotlin, so it has no bridge header")
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

    private fun createPackageManifest(packageModules: List<SwiftPackageModule>) {
        val manifest = packagePath.getFile().resolve("Package.swift")
        val cinteropImport = if (
            swiftPMImportHasDependencies.get() && swiftPMImportProductName.isPresent && swiftPMImportPackageRoot.isPresent
        ) {
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
            swiftApiModule, swiftLibrary, kotlinRuntimeModule, packageModules, cinteropImport, kotlinBinaryTargetName.orNull,
            platforms.get()
        )
        manifest.writeText(content)
    }

    private fun appendToOtherIncludes(name: String, path: File) {
        if (!collectIncludes.get()) return
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
        modules: List<SwiftPackageModule>,
        cinteropImport: CinteropPackageImport? = null,
        kotlinBinaryTarget: String? = null,
        platforms: List<SwiftPackagePlatform> = emptyList(),
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
                            emitListItems(platforms.map { ".${it.name}(\"${it.minimumVersion}\")" })
                        }
                    }
                }
                entry {
                    block("products: [", "]") {
                        block(".library(", ")") {
                            commaSeparatedEntries {
                                entry { line("name: \"$swiftLibrary\"") }
                                entry { line("targets: [${modules.map { it.module }.productTargets().joinToString(", ")}]") }
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
        dependencyPlatforms: Map<String, Set<Family>> = emptyMap(),
    ) {
        block(".target(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$name\"") }
                val deps = (dependencies?.map { targetDependency(it, dependencyPlatforms[it]) } ?: emptyList()) + rawDependencies
                if (deps.isNotEmpty()) {
                    entry { line("dependencies: [${deps.joinToString(", ")}]") }
                }
            }
        }
    }

    private fun targetDependency(name: String, families: Set<Family>?): String {
        if (families == null) return "\"$name\""
        val platforms = swiftPackagePlatformNames.filterKeys { it in families }.values
        return ".target(name: \"$name\", condition: .when(platforms: [${platforms.joinToString(", ") { ".$it" }}]))"
    }

    private fun CommaSeparatedEntriesBuilder.emitTargetDefinitions(
        modules: List<SwiftPackageModule>,
        kotlinRuntime: String,
        cinteropProductExpression: String?,
    ) {
        // The reexported cinterop's `import`s live in the Swift API targets, so each gets the product dependency.
        val rawDependencies = listOfNotNull(cinteropProductExpression)
        modules.forEach { packageModule ->
            val module = packageModule.module
            entry { emitTarget(module.name, module.spmDependencies(kotlinRuntime), rawDependencies, packageModule.dependencyPlatforms) }
            if (module is GradleSwiftExportModule.BridgesToKotlin) {
                entry { emitTarget(module.bridgeName) }
            }
        }
    }
}
