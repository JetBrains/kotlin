/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.ModuleMapGenerator
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftImportFingerprintedCoordinationService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.packageArguments
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.productArguments
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.toSwiftPMPlatforms
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
import org.jetbrains.kotlin.gradle.utils.ensurePathComponentSafeString
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import org.jetbrains.kotlin.gradle.utils.resolveInside
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
    @get:Input
    abstract val target: Property<KonanTarget>

    /** The output directory of the run: the modules file and the generated files it lists. */
    @get:InputDirectory
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val swiftModulesFile: Provider<RegularFile>
        get() = outputDirectory.swiftModulesFile
}

internal fun ObjectFactory.SwiftExportTargetOutput(
    target: KotlinNativeTarget,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): SwiftExportTargetOutput = newInstance<SwiftExportTargetOutput>().apply {
    this.target.set(target.konanTarget)
    outputDirectory.set(swiftExportTask.flatMap { it.parameters.outputDirectory })
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
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinRuntime: DirectoryProperty

    /** The output directory of the run: the modules file and the generated files it lists. Xcode flow only. */
    @get:Optional
    @get:InputDirectory
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftExportOutputDirectory: DirectoryProperty

    @get:Internal
    val swiftModulesFile: Provider<RegularFile>
        get() = swiftExportOutputDirectory.swiftModulesFile

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
    @get:NormalizeLineEndings
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val swiftPMImportFingerprint: RegularFileProperty

    @get:Internal
    abstract val swiftPMImportCoordinationService: Property<SwiftImportFingerprintedCoordinationService>

    /**
     * The packages of the SwiftPM import, declared in the manifest as the project declares them, so the consumer
     * of the package resolves and links them. Empty in the Xcode flow, which links them through the synthetic
     * package in [swiftPMImportPackageRoot] instead.
     */
    @get:Input
    abstract val swiftPMDependencies: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val transitiveSwiftPMDependencies: MapProperty<String, Set<SwiftPMDependency>>

    /**
     * Where the package is exported to, if it is: the local packages of [swiftPMDependencies] are referenced
     * relative to it, so the exported package stays valid when the project moves.
     */
    @get:Optional
    @get:Input
    abstract val exportDirectoryPath: Property<String>

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

    @get:Internal
    val subpackagesPath: Provider<Directory>
        get() = packagePath.dir(SUBPACKAGES)

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
        fileSystem.delete { it.delete(sourcesPath, includesPath, subpackagesPath) }
        createSPMSources(packageModules, packageTargets = targetModules.map { it.target }.toSet())
        createPackageManifest(packageModules)
        createKotlinRuntimeTarget()
    }

    private fun deserializeSwiftModules(): List<GradleSwiftExportModule> {
        check(swiftExportOutputDirectory.isPresent) { "Neither targetOutputs nor swiftExportOutputDirectory is set for $path" }
        return readSwiftModules(swiftModulesFile.getFile())
    }

    private fun deserializeTargetModules(): List<SwiftExportTargetModules> = targetOutputs.get().map { output ->
        SwiftExportTargetModules(
            output.target.get(),
            readSwiftModules(output.swiftModulesFile.getFile()),
        )
    }

    private fun readSwiftModules(modulesFile: File): List<GradleSwiftExportModule> =
        SerializationTools.readFromJson(modulesFile.readText(), baseDirectory = modulesFile.parentFile).modules

    /**
     * Writes the [source] file of [packageModule] into [destination], combined from its variants if there are any.
     */
    private fun createSource(
        packageModule: SwiftPackageModule,
        packageTargets: Set<KonanTarget>,
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

    private fun createSPMSources(packageModules: List<SwiftPackageModule>, packageTargets: Set<KonanTarget>) {
        packageModules.forEach { packageModule ->
            val module = packageModule.module
            val swiftModulePath = sourcesPath.getFile().resolveInside(module.name).apply { createDirectory() }
            createSource(packageModule, packageTargets, swiftModulePath, SwiftPackageSourceLanguage.SWIFT) { it.swiftApiFile }

            if (module is GradleSwiftExportModule.BridgesToKotlin) {
                val bridgeModulePath = sourcesPath.getFile().resolveInside(ensurePathComponentSafeString(module.bridgeName)).apply { createDirectory() }
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
        val content = SPMManifestGenerator.generateManifest(
            swiftApiModule, swiftLibrary, kotlinRuntimeModule, packageModules,
            listOfNotNull(syntheticPackageDependency()) + importedPackageDependencies() + createSwiftPMImportSubpackages(),
            kotlinBinaryTargetName.orNull,
            platforms.get()
        )
        manifest.writeText(content)
    }

    private fun createSwiftPMImportSubpackages(): List<SwiftPackageDependency> {
        val exportedPackageRoot = exportDirectoryPath.orNull?.let { File(it).normalizedAbsoluteFile() }
        return transitiveSwiftPMDependencies.get().toSortedMap().map { (identifier, dependencies) ->
            val subpackage = subpackagesPath.get().asFile.resolve(identifier)
            subpackage.resolve("$SOURCES_DIRECTORY/$identifier").apply { createDirectory() }
                .resolve("$identifier.swift").writeText("")
            subpackage.resolve("Package.swift").writeText(
                SPMManifestGenerator.generateImportedPackagesManifest(
                    packageName = identifier,
                    platforms = platforms.get(),
                    packageDependencies = importedPackageDeclarations(
                        swiftPMDependencies = dependencies,
                        packageRoot = exportedPackageRoot?.resolve("$SUBPACKAGES/$identifier"),
                    ),
                )
            )
            SwiftPackageDependency(
                packageDeclaration = ".package(path: \"$SUBPACKAGES/$identifier\")",
                productDeclarations = listOf(".product(name: \"$identifier\", package: \"$identifier\")"),
            )
        }
    }

    private fun syntheticPackageDependency(): SwiftPackageDependency? {
        if (!swiftPMImportHasDependencies.get() || !swiftPMImportProductName.isPresent || !swiftPMImportPackageRoot.isPresent) {
            return null
        }
        val root = swiftPMImportFingerprint.orNull?.asFile
            ?.let { swiftPMImportCoordinationService.get().sharedPackageRootFor(it) }
            ?: swiftPMImportPackageRoot.getFile()
        return SwiftPackageDependency(
            packageDeclaration = ".package(path: \"${root.absolutePath}\")",
            productDeclarations = listOf(".product(name: \"${swiftPMImportProductName.get()}\", package: \"${root.name}\")"),
        )
    }

    private fun importedPackageDependencies(): List<SwiftPackageDependency> = importedPackageDeclarations(
        swiftPMDependencies = swiftPMDependencies.get(),
        packageRoot = exportDirectoryPath.orNull?.let { File(it).normalizedAbsoluteFile() },
    )

    private fun importedPackageDeclarations(
        swiftPMDependencies: Set<SwiftPMDependency>,
        packageRoot: File?,
    ): List<SwiftPackageDependency> {
        val umbrellaPlatforms = umbrellaPlatforms()
        return swiftPMDependencies.sortedBy { it.packageName }.map { dependency ->
            SwiftPackageDependency(
                packageDeclaration = ".package(${dependency.packageArguments(packageRoot).joinToString(", ")})",
                productDeclarations = dependency.products.map { product ->
                    ".product(${product.productArguments(dependency.packageName, null, umbrellaPlatforms).joinToString(", ")})"
                },
            )
        }
    }

    private fun umbrellaPlatforms(): Set<SwiftPMDependency.Platform> =
        targetOutputs.get().map { it.target.get() }.toSet().toSwiftPMPlatforms()

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
        const val SUBPACKAGES = "subpackages"
    }
}

/**
 * A package the generated package depends on: its `.package(...)` entry and the `.product(...)` entries the API
 * targets take from it.
 */
internal data class SwiftPackageDependency(
    val packageDeclaration: String,
    val productDeclarations: List<String>,
)

internal object SPMManifestGenerator {

    /**
     * The modules Swift Export writes from its own templates. Keep in sync with the defaults of `SwiftExportConfig`
     * in `swift-export-standalone` (`moduleForPackagesName`, `runtimeSupportModuleName`, `coroutineSupportModuleName`,
     * `runtimeModuleName`): the names are not passed to the plugin, and nothing in the modules file marks them.
     */
    private val swiftExportSupportModules = setOf("ExportedKotlinPackages", "KotlinRuntimeSupport", "KotlinCoroutineSupport", "KotlinRuntime")

    fun generateManifest(
        swiftApiModule: String,
        swiftLibrary: String,
        kotlinRuntime: String,
        modules: List<SwiftPackageModule>,
        packageDependencies: List<SwiftPackageDependency> = emptyList(),
        kotlinBinaryTarget: String? = null,
        platforms: List<SwiftPackagePlatform> = emptyList(),
    ): String = buildStringBlock {
        line("// swift-tools-version: 6.1")
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
                if (packageDependencies.isNotEmpty()) {
                    entry {
                        block("dependencies: [", "]") {
                            emitListItems(packageDependencies.map { it.packageDeclaration })
                        }
                    }
                }
                entry {
                    block("targets: [", "]") {
                        commaSeparatedEntries {
                            if (kotlinBinaryTarget != null) {
                                entry { emitBinaryTarget(kotlinBinaryTarget) }
                            }
                            emitTargetDefinitions(modules, kotlinRuntime, packageDependencies.flatMap { it.productDeclarations })
                            entry { emitTarget(kotlinRuntime, dependencies = listOfNotNull(kotlinBinaryTarget)) }
                        }
                    }
                }
                // Tools version 6 defaults to the Swift 6 language mode, and the generated sources don't pass its
                // concurrency checks yet.
                entry { line("swiftLanguageModes: [.v5]") }
            }
        }
    }

    fun generateImportedPackagesManifest(
        packageName: String,
        platforms: List<SwiftPackagePlatform>,
        packageDependencies: List<SwiftPackageDependency>,
    ): String = buildStringBlock {
        line("// swift-tools-version: 6.1")
        line()
        line("import PackageDescription")
        block("let package = Package(", ")") {
            commaSeparatedEntries {
                entry { line("name: \"$packageName\"") }
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
                                entry { line("name: \"$packageName\"") }
                                entry { line("targets: [\"$packageName\"]") }
                            }
                        }
                    }
                }
                if (packageDependencies.isNotEmpty()) {
                    entry {
                        block("dependencies: [", "]") {
                            emitListItems(packageDependencies.map { it.packageDeclaration })
                        }
                    }
                }
                entry {
                    block("targets: [", "]") {
                        commaSeparatedEntries {
                            entry {
                                emitTarget(
                                    packageName,
                                    rawDependencies = packageDependencies.flatMap { it.productDeclarations },
                                )
                            }
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
        productDependencies: List<String>,
    ) {
        // The `import`s of the imported packages live in the Swift API targets, so each gets the product dependencies.
        // The support modules are generated from fixed templates and never refer to an imported type.
        modules.forEach { packageModule ->
            val module = packageModule.module
            val moduleProductDependencies = if (module.name in swiftExportSupportModules) emptyList() else productDependencies
            entry {
                emitTarget(
                    module.name,
                    module.spmDependencies(kotlinRuntime),
                    moduleProductDependencies,
                    packageModule.dependencyPlatforms
                )
            }
            if (module is GradleSwiftExportModule.BridgesToKotlin) {
                entry { emitTarget(module.bridgeName) }
            }
        }
    }
}
