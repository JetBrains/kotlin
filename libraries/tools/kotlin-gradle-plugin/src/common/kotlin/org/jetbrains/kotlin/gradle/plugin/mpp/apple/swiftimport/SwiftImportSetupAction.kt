package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.property
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.projectPathOrNull
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import java.security.MessageDigest
import java.util.UUID
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


internal val SwiftImportSetupAction = KotlinProjectSetupAction {
    val kotlinExtension = project.multiplatformExtension
    kotlinExtension.extensions.create(
        SwiftImportExtension.EXTENSION_NAME,
        SwiftImportExtension::class.java
    )
    val swiftPMImportExtension = kotlinExtension.getExtension<SwiftImportExtension>(
        SwiftImportExtension.EXTENSION_NAME
    )!!

    val swiftPMDependenciesMetadata = project.registerTask<SerializeSwiftPMDependenciesMetadata>(
        SerializeSwiftPMDependenciesMetadata.TASK_NAME,
    )
    project.configurations.createConsumable("swiftPMDependenciesMetadataElements") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        outgoing.artifact(swiftPMDependenciesMetadata)
    }

    val implementationDependencies = configurations.getByName(kotlinExtension.sourceSets.commonMain.get().implementationConfigurationName)
    val apiDependencies = configurations.getByName(kotlinExtension.sourceSets.commonMain.get().apiConfigurationName)

    val swiftPmDependenciesMetadataClasspath = project.configurations.createResolvable("swiftPMDependenciesMetadataClasspath") {
        // 1. Select metadataApiElements graph
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KOTLIN_METADATA))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        extendsFrom(implementationDependencies)
        extendsFrom(apiDependencies)
    }.incoming.artifactView {
        it.withVariantReselection()
        it.lenient(true)
        it.attributes {
            // 2. Reselect SwiftPM metadata variant
            it.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_USAGE))
            it.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
    }
    val transitiveSwiftPMDependenciesMap = swiftPMDependencies(swiftPmDependenciesMetadataClasspath)

    val syntheticImportProjectGenerationTaskForEmbedAndSignLinkage = regenerateLinkageImportProjectTask()
    syntheticImportProjectGenerationTaskForEmbedAndSignLinkage.configure {
        it.configureWithExtension(swiftPMImportExtension)
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesMap)
    }

    val projectPathProvider = project.providers.environmentVariable(IntegrateLinkagePackageIntoXcodeProject.PROJECT_PATH_ENV)

    val syntheticImportProjectGenerationTaskForLinkageForCli = locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
        lowerCamelCaseName(
            GenerateSyntheticLinkageImportProject.TASK_NAME,
            "forLinkageForCli",
        ),
    ) {
        it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesMap)
        it.configureWithExtension(swiftPMImportExtension)
        it.syntheticImportProjectRoot.set(
            projectPathProvider.flatMap {
                project.layout.dir(
                    project.provider { File(it).parentFile.resolve(SYNTHETIC_IMPORT_TARGET_MAGIC_NAME) }
                )
            }
        )
    }

    project.locateOrRegisterTask<IntegrateLinkagePackageIntoXcodeProject>(IntegrateLinkagePackageIntoXcodeProject.TASK_NAME) {
        it.dependsOn(syntheticImportProjectGenerationTaskForLinkageForCli)
        it.xcodeprojPath.set(projectPathProvider)
    }

    swiftPMImportExtension.spmDependencies.all { swiftPMDependency ->
        kotlinPropertiesProvider.enableCInteropCommonizationSetByExternalPlugin = true
        val syntheticImportProjectGenerationTaskForCinterops = project.locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
            lowerCamelCaseName(
                GenerateSyntheticLinkageImportProject.TASK_NAME,
                "forCinterops",
            ),
        ) {
            it.configureWithExtension(swiftPMImportExtension)
            it.dependencyIdentifierToImportedSwiftPMDependencies.set(transitiveSwiftPMDependenciesMap)
        }

        val syntheticImportTasks = listOf(
            syntheticImportProjectGenerationTaskForCinterops,
            syntheticImportProjectGenerationTaskForLinkageForCli,
            syntheticImportProjectGenerationTaskForEmbedAndSignLinkage,
        )
        syntheticImportTasks.forEach { it.configure { it.directlyImportedSpmModules.add(swiftPMDependency) } }
        swiftPMDependenciesMetadata.configure { it.importedSpmModules.add(swiftPMDependency) }

        val packageFetchTask = project.locateOrRegisterTask<FetchSyntheticImportProjectPackages>(
            FetchSyntheticImportProjectPackages.TASK_NAME,
        ) {
            it.dependsOn(syntheticImportProjectGenerationTaskForCinterops)
            it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinterops.map { it.syntheticImportProjectRoot.get() })
        }

        kotlinExtension.targets.matching {
            val targetSupportsSwiftPMImport = it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily
            targetSupportsSwiftPMImport
        }.all { target ->
            target as KotlinNativeTarget
            syntheticImportTasks.forEach {
                it.configure {
                    it.konanTargets.add(target.konanTarget)
                }
            }
            val mainCompilationCinterops = target.compilations.getByName("main").cinterops
            val cinteropName = "swiftPMImport"
            val targetPlatform = target.konanTarget.applePlatform
            // use sdk for a more conventional name
            val targetSdk = target.konanTarget.appleTarget.sdk
            val defFilesGenerationTask = project.locateOrRegisterTask<ConvertSyntheticSwiftPMImportProjectIntoDefFile>(
                lowerCamelCaseName(
                    ConvertSyntheticSwiftPMImportProjectIntoDefFile.TASK_NAME,
                    targetSdk,
                )
            ) {
                // FIXME: Remove this and fix input/outputs
                it.dependsOn(packageFetchTask)
                it.resolvedPackagesState.set(packageFetchTask.map{ it.swiftPMDependenciesCheckoutWorkspaceFile.get() })
                it.xcodebuildPlatform.set(targetPlatform)
                it.xcodebuildSdk.set(targetSdk)
                it.swiftPMDependenciesCheckout.set(packageFetchTask.map { it.swiftPMDependenciesCheckout.get() })
                it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTaskForCinterops.map { it.syntheticImportProjectRoot.get() })
            }

            defFilesGenerationTask.configure {
                it.clangModules.addAll(swiftPMDependency.cinteropClangModules)
            }

            if (cinteropName !in mainCompilationCinterops.names) {
                defFilesGenerationTask.configure {
                    it.architectures.add(target.konanTarget.architecture)
                }
                mainCompilationCinterops.create(cinteropName).definitionFile.set(
                    defFilesGenerationTask.map {
                        it.defFilePath(target.konanTarget.architecture).get()
                    }
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun swiftPMDependencies(swiftPmDependenciesMetadataClasspath: ArtifactView): Provider<Map<String, Set<SwiftPMDependency>>> {
    return swiftPmDependenciesMetadataClasspath
        .artifacts.resolvedArtifacts
        .map { artifacts ->
            artifacts.associate { resolvedArtifact ->
                val swiftPMPackageIdentifier = when (val componentId = resolvedArtifact.id.componentIdentifier) {
                    is ProjectComponentIdentifier -> componentId.projectPath.replace(Regex("[^a-zA-Z0-9]"), "_")
                    is ModuleComponentIdentifier -> "${componentId.group}_${componentId.module}_${componentId.version}".replace(Regex("[^a-zA-Z0-9]"), "_")
                    else -> error("Unexpected componentId: $componentId")
                }
                swiftPMPackageIdentifier to resolvedArtifact.file.inputStream().use {
                    ObjectInputStream(it).readObject() as Set<SwiftPMDependency>
                }
            }
        }
}

/**
 * FIXME: This is incorrect, the linkage package should:
 * - collect dependencies from all the entire classpath
 * - should emit the linkage structure at specific sites, e.g. for embedAndSign, for internal linkage, etc
 */
internal fun Project.regenerateLinkageImportProjectTask(): TaskProvider<GenerateSyntheticLinkageImportProject> = locateOrRegisterTask<GenerateSyntheticLinkageImportProject>(
    lowerCamelCaseName(
        GenerateSyntheticLinkageImportProject.TASK_NAME,
        "forLinkage",
    ),
)

@DisableCachingByDefault(because = "...")
internal abstract class GenerateSyntheticLinkageImportProject : DefaultTask() {

    @get:Input
    abstract val directlyImportedSpmModules: SetProperty<SwiftPMDependency>

    @get:Input
    abstract val dependencyIdentifierToImportedSwiftPMDependencies: MapProperty<String, Set<SwiftPMDependency>>

    @get:OutputDirectory
    open val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport")
    )

    @get:Input
    abstract val konanTargets: SetProperty<KonanTarget>

    @get:Input
    abstract val iosDeploymentVersion: Property<String>
    @get:Input
    abstract val macosDeploymentVersion: Property<String>
    @get:Input
    abstract val watchosDeploymentVersion: Property<String>
    @get:Input
    abstract val tvosDeploymentVersion: Property<String>

    @get:Inject
    protected abstract val execOps: ExecOperations

    fun configureWithExtension(swiftPMImportExtension: SwiftImportExtension) {
        iosDeploymentVersion.set(swiftPMImportExtension.iosDeploymentVersion)
        macosDeploymentVersion.set(swiftPMImportExtension.macosDeploymentVersion)
        watchosDeploymentVersion.set(swiftPMImportExtension.watchosDeploymentVersion)
        tvosDeploymentVersion.set(swiftPMImportExtension.tvosDeploymentVersion)
    }

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        val packageRoot = syntheticImportProjectRoot.get().asFile
        generatePackageManifest(
            identifier = SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
            packageRoot = packageRoot,
            directlyImportedSwiftPMDependencies = directlyImportedSpmModules.get(),
            localPackages = dependencyIdentifierToImportedSwiftPMDependencies.get().keys,
        )
        dependencyIdentifierToImportedSwiftPMDependencies.get().forEach { (dependencyIdentifier, swiftPMDependencies) ->
            generatePackageManifest(
                identifier = dependencyIdentifier,
                packageRoot = packageRoot.resolve("${SUBPACKAGES}/${dependencyIdentifier}"),
                directlyImportedSwiftPMDependencies = swiftPMDependencies,
                localPackages = setOf(),
            )
        }
    }

    private fun generatePackageManifest(
        identifier: String,
        packageRoot: File,
        directlyImportedSwiftPMDependencies: Set<SwiftPMDependency>,
        // FIXME: Implicitly, this is the directory, package and product name
        localPackages: Set<String>,
    ) {
        val repoDependencies = (directlyImportedSwiftPMDependencies.map {
            ".package(url: \"${it.repository}\", from: \"${it.fromVersion}\"),"
        } + localPackages.map {
            ".package(path: \"${SUBPACKAGES}/${it}\"),"
        })
        val targetDependencies = (directlyImportedSwiftPMDependencies.flatMap { dep -> dep.products.map { it to dep.packageName } }.map {
            ".product(name: \"${it.first}\", package: \"${it.second}\"),"
        } + localPackages.map {
            ".product(name: \"${it}\", package: \"${it}\"),"
        })

        val platforms = konanTargets.get().map { it.family }.toSet().map {
            when (it) {
                Family.OSX -> ".macOS(\"${macosDeploymentVersion.get()}\"),"
                Family.IOS -> ".iOS(\"${iosDeploymentVersion.get()}\"),"
                Family.TVOS -> ".tvOS(\"${tvosDeploymentVersion.get()}\"),"
                Family.WATCHOS -> ".watchOS(\"${watchosDeploymentVersion.get()}\"),"
                Family.LINUX,
                Family.MINGW,
                Family.ANDROID
                    -> error("???")
            }
        }

        val manifest = packageRoot.resolve(MANIFEST_NAME)
        manifest.also {
            it.parentFile.mkdirs()
        }.writeText(
            buildString {
                appendLine("// swift-tools-version: 5.9")
                appendLine("import PackageDescription")
                appendLine("let package = Package(")
                appendLine("  name: \"$identifier\",")
                appendLine("  platforms: [")
                platforms.forEach { appendLine("    $it")}
                appendLine("  ],")
                appendLine(
                    """
                        products: [
                            .library(
                                name: "$identifier",
                                targets: ["$identifier"],
                            ),
                        ],
                    """.replaceIndent("  ")
                )
                appendLine("  dependencies: [")
                repoDependencies.forEach { appendLine("    $it") }
                appendLine("  ],")
                appendLine("  targets: [")
                appendLine("    .target(")
                appendLine("      name: \"$identifier\",")
                appendLine("      dependencies: [")
                targetDependencies.forEach { appendLine("        $it") }
                appendLine("      ]")
                appendLine("    ),")
                appendLine("  ]")
                appendLine(")")
            }
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

    companion object {
        const val TASK_NAME = "generateSyntheticLinkageSwiftPMImportProject"
        const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME = "_internal_linkage_SwiftPMImport"
        const val SUBPACKAGES = "subpackages"
        const val MANIFEST_NAME = "Package.swift"
    }
}

@DisableCachingByDefault(because = "...")
internal abstract class FetchSyntheticImportProjectPackages : DefaultTask() {

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty()

    @get:InputFile
    protected val manifest get() = syntheticImportProjectRoot.map { it.file(GenerateSyntheticLinkageImportProject.MANIFEST_NAME) }

    // FIXME: Actually think about: "what do we want as a UTD check for the the packages checkout? The lock file?"
    // FIXME: We probably want this cache to be global
    @get:Internal
    val swiftPMDependenciesCheckout: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckout")
    )

    @get:OutputFile
    val swiftPMDependenciesCheckoutWorkspaceFile = swiftPMDependenciesCheckout.file("workspace-state.json")

    @get:Internal
    protected val swiftPMDependenciesCheckoutLogs: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftPMCheckoutDD")
    )

    @get:Inject
    protected abstract val execOps: ExecOperations

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        checkoutSwiftPMDependencies()
    }

    private fun checkoutSwiftPMDependencies() {
        execOps.exec {
            it.workingDir(syntheticImportProjectRoot.get().asFile)
            it.commandLine(
                "xcodebuild", "-resolvePackageDependencies",
                "-scheme", SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, swiftPMDependenciesCheckout.get().asFile.path,
                "-derivedDataPath", swiftPMDependenciesCheckoutLogs.get().asFile.path,
            )
        }
    }

    companion object {
        const val TASK_NAME = "fetchSyntheticImportProjectPackages"
    }
}

@DisableCachingByDefault(because = "...")
internal abstract class ConvertSyntheticSwiftPMImportProjectIntoDefFile : DefaultTask() {

    @get:Input
    abstract val xcodebuildPlatform: Property<String>
    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<Architecture>

    @get:Input
    abstract val clangModules: SetProperty<String>

    @get:InputFile
    abstract val resolvedPackagesState: RegularFileProperty

    @get:OutputDirectory
    protected val defFiles = xcodebuildSdk.flatMap { sdk ->
        project.layout.buildDirectory.dir("kotlin/swiftImportDefs/${sdk}")
    }

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Internal
    val syntheticImportDd = project.layout.buildDirectory.dir("kotlin/swiftImportDd")

    @get:Inject
    protected abstract val execOps: ExecOperations

    @get:Inject
    protected abstract val objects: ObjectFactory

    private val layout = project.layout

    private val cinteropNamespace = listOf(
        "swiftPMImport",
        project.group.toString(),
        if (project.path == ":") project.name else project.path.drop(1)
    ).filter {
        !it.isEmpty()
    }.joinToString(".") {
        it.replace(Regex("[^a-zA-Z0-9_.]"), ".")
    }

    @TaskAction
    fun generateDefFiles() {
        val dumpIntermediates = xcodebuildSdk.flatMap { sdk ->
            layout.buildDirectory.dir("kotlin/swiftImportClangDump/${sdk}")
        }.get().asFile.also { it.mkdirs() }

        val clangArgsDumpScript = dumpIntermediates.resolve("clangDump.sh")
        clangArgsDumpScript.writeText(searchPathsDumpScript())
        clangArgsDumpScript.setExecutable(true)
        val clangArgsDump = dumpIntermediates.resolve("clang_args_dump")
        if (clangArgsDump.exists()) {
            clangArgsDump.deleteRecursively()
        }
        clangArgsDump.mkdirs()

        val targetArchitectures = architectures.get().map {
            clangArchitecture(it)
        }

        val projectRoot = syntheticImportProjectRoot.get().asFile
        // FIXME: For some reason reusing dd in parallel xcodebuild calls explodes something in Xcode
        val dd = syntheticImportDd.get().asFile.resolve("dd_${xcodebuildSdk.get()}")
        val forceClangToReexecute = dd.resolve("Build/Intermediates.noindex/$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME.build")
        if (forceClangToReexecute.exists()) {
            forceClangToReexecute.deleteRecursively()
        }

        execOps.exec { exec ->
            exec.workingDir(projectRoot)
            exec.commandLine(
                "xcodebuild", "build",
                "-scheme", SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                "-destination", "generic/platform=${xcodebuildPlatform.get()}",
                "-derivedDataPath", dd.path,
                XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, swiftPMDependenciesCheckout.get().asFile.path,
                "CC=${clangArgsDumpScript.path}",
                "ARCHS=${targetArchitectures.joinToString(" ")}",
                // FIXME: Check how truly necessary this is
                "CODE_SIGN_IDENTITY=",
            )
            exec.environment(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV, clangArgsDump)

            val environmentToFilter = listOf(
                "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES",
            ) + AppleSdk.xcodeEnvironmentDebugDylibVars
            environmentToFilter.forEach {
                if (exec.environment.containsKey(it)) {
                    exec.environment.remove(it)
                }
            }
            // FIXME: Xcodebuild passes these from env to somewhere
            exec.environment.keys.filter {
                it.startsWith("OTHER_")
            }.forEach {
                exec.environment.remove(it)
            }
        }

        architectures.get().forEach { architecture ->
            val clangArchitecture = clangArchitecture(architecture)
            val architectureSpecificProductClangCalls = clangArgsDump.listFiles().filter {
                it.isFile
            }.filter {
                val clangArgs = it.readLines().single()
                "-fmodule-name=${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}" in clangArgs
                        && "-target${CLANG_ARGS_SEPARATOR}${clangArchitecture}-apple" in clangArgs
            }
            val architectureSpecificProductClangCall = architectureSpecificProductClangCalls.single()
            val searchPathStrings = architectureSpecificProductClangCall.readLines().single().split(CLANG_ARGS_SEPARATOR).filter { arg ->
                arg.startsWith("-F") || arg.startsWith("-I") || arg.startsWith("-fmodule-map-file=")
            }.toSet()

            val defFileSearchPaths = searchPathStrings.joinToString(" ") { "\"${it}\"" }
            val modules = clangModules.get().joinToString(" ") { "\"${it}\"" }

            val workaroundKT81695 = "-DSWIFT_TYPEDEFS"
            val defFilePath = defFilePath(architecture)
            defFilePath.getFile().writeText(
                """
                    language = Objective-C
                    modules = $modules
                    compilerOpts = $workaroundKT81695 -fmodules $defFileSearchPaths
                    package = $cinteropNamespace
                """.trimIndent()
            )
        }
    }

    fun defFilePath(architecture: Architecture) = defFiles.map { it.file("${architecture.name}.def") }

    // FIXME: Fix watchos and use some different mapping here
    private fun clangArchitecture(architecture: Architecture) = when (architecture) {
        Architecture.X64 -> "x86_64"
        Architecture.ARM64 -> "arm64"
        Architecture.X86,
        Architecture.ARM32
            -> error("???")
    }

    private fun searchPathsDumpScript() = """
        #!/bin/bash

        DUMP_FILE="${'$'}{${KOTLIN_CLANG_ARGS_DUMP_FILE_ENV}}/${'$'}(/usr/bin/uuidgen)"
        for arg in "$@"
        do
           echo -n "${'$'}arg" >> "${'$'}{DUMP_FILE}"
           echo -n "$CLANG_ARGS_SEPARATOR" >> "${'$'}{DUMP_FILE}"
        done

        clang "$@"
    """.trimIndent()

    companion object {
        const val KOTLIN_CLANG_ARGS_DUMP_FILE_ENV = "KOTLIN_CLANG_ARGS_DUMP_FILE"
        const val CLANG_ARGS_SEPARATOR = ";"
        const val TASK_NAME = "convertSyntheticImportProjectIntoDefFile"
    }

}

@DisableCachingByDefault(because = "...")
internal abstract class IntegrateLinkagePackageIntoXcodeProject : DefaultTask() {

    @get:Input
    abstract val xcodeprojPath: Property<String>

    @get:Inject
    protected abstract val execOps: ExecOperations

    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun integrate() {
        val projectPath = File(xcodeprojPath.get())
        val pbxprojPath = projectPath.resolve("project.pbxproj")
        val output = ByteArrayOutputStream()
        execOps.exec {
            it.standardOutput = output
            it.commandLine(
                "/usr/bin/plutil",
                "-convert", "json",
                pbxprojPath,
                "-o", "-"
            )
        }

        val projectJson = Gson().fromJson(
            output.toString(), Map::class.java
        ) as Map<String, Any>
        if (isLinkageProductReferencedInPBXObjects(projectJson)) {
            println("Product already referenced, nothing to do")
            return
        }

        val rootProjectId = projectJson.property<String>("rootObject")

        val objects = projectJson.property<Map<String, Any>>("objects").toMutableMap()
        val rootProject = objects.entries.single { it.key == rootProjectId }

        val productDependencyReference = generateRandomPBXObjectReference()
        objects[productDependencyReference] = mapOf(
            "isa" to "XCSwiftPackageProductDependency",
            "productName" to SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
        )

        val buildFileDependencyReference = generateRandomPBXObjectReference()
        objects[buildFileDependencyReference] = mapOf(
            "isa" to "PBXBuildFile",
            "productRef" to productDependencyReference,
        )

        val localPackageReference = generateRandomPBXObjectReference()
        objects[localPackageReference] = mapOf(
            "isa" to "XCLocalSwiftPackageReference",
            "relativePath" to SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
        )

        val embedAndSignShellScriptPhaseReference = objects.entries.first { (_, pbxObject) ->
            if (pbxObject is Map<*, *>) {
                pbxObject as Map<String, Any>
                val shellContent = pbxObject["shellScript"]
                if (shellContent is String) {
                    "gradle" in shellContent
                } else false
            } else false
        }.key

        val embedAndSignTargets = objects.entries.filter { (_, pbxObject) ->
            if (pbxObject is Map<*, *>) {
                pbxObject as Map<String, Any>
                val phases = pbxObject["buildPhases"]
                if (phases is List<*>) {
                    phases as List<String>
                    embedAndSignShellScriptPhaseReference in phases
                } else false
            } else false
        }

        val updatedProject = (rootProject.value as Map<String, Any>).toMutableMap()
        val existingPackages = updatedProject["packageReferences"] as? List<String> ?: listOf()
        updatedProject["packageReferences"] = existingPackages + localPackageReference
        objects[rootProject.key] = updatedProject

        embedAndSignTargets.forEach { (uuid, target) ->
            val updatedTarget = (target as Map<String, Any>).toMutableMap()
            val existingPackageProductDependencies = updatedProject["packageProductDependencies"] as? List<String> ?: listOf()
            updatedTarget["packageProductDependencies"] = existingPackageProductDependencies + productDependencyReference
            objects[uuid] = updatedTarget
        }

        embedAndSignTargets.mapNotNull { (_, target) ->
            target as Map<String, Any>
            target.property<List<String>>("buildPhases")
        }.flatten().forEach { buildPhaseReference ->
            val buildPhase = objects.property<Map<String, Any>>(buildPhaseReference)
            if (buildPhase.property<String>("isa") == "PBXFrameworksBuildPhase") {
                val updatedBuildPhase = buildPhase.toMutableMap()
                val existingFiles = updatedBuildPhase["files"] as? List<String> ?: listOf()
                updatedBuildPhase["files"] = existingFiles + buildFileDependencyReference
                objects[buildPhaseReference] = updatedBuildPhase
            }
        }

        val updatedProjectJson = projectJson.toMutableMap()
        updatedProjectJson["objects"] = objects

        val resultingJson = Gson().toJson(updatedProjectJson)
        pbxprojPath.writeText(resultingJson)
    }

    private fun generateRandomPBXObjectReference(): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        return messageDigest.digest(
            UUID.randomUUID().toString().toByteArray()
        ).joinToString(separator = "") { byte -> "%02x".format(byte) }.uppercase().subSequence(0, 24).toString()
    }

    companion object {
        const val PROJECT_PATH_ENV = "XCODEPROJ_PATH"
        const val TASK_NAME = "integrateLinkagePackage"
    }
}

internal abstract class SerializeSwiftPMDependenciesMetadata : DefaultTask() {

    @get:Input
    abstract val importedSpmModules: SetProperty<SwiftPMDependency>

    @get:OutputFile
    val serializationFile: Provider<RegularFile> = project.layout.buildDirectory.file("kotlin/importedSpmModules")

    @TaskAction
    fun serialize() {
        val spmDependencies = importedSpmModules.get()
            // get rid of Google set
            .map { it }.toSet()
        serializationFile.get().asFile.outputStream().use { file ->
            ObjectOutputStream(file).use { objects ->
                objects.writeObject(spmDependencies)
            }
        }
    }

    companion object {
        const val TASK_NAME = "serializeSwiftPMDependenciesMetadata"
    }

}

fun isLinkageProductReferencedInPBXObjects(projectJson: Map<String, Any>): Boolean {
    val objects = projectJson.property<Map<String, Any>>("objects")
    // FIXME: Check if the product is correctly integrated into the build phase
    val hasSyntheticImportProjectReference = objects.values.any { pbxObject ->
        @Suppress("UNCHECKED_CAST")
        pbxObject as Map<String, Any>
        val type = pbxObject.property<String>("isa")
        if (type == "XCSwiftPackageProductDependency") {
            val packageProductName = pbxObject.property<String>("productName")
            packageProductName == SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
        } else false
    }
    return hasSyntheticImportProjectReference
}

const val XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER = "-clonedSourcePackagesDirPath"
const val SWIFTPM_DEPENDENCIES_METADATA_USAGE = "SWIFTPM_DEPENDENCIES_METADATA"