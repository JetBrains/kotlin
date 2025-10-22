package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticImportProjectAndFetchPackages.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

internal val SwiftImportSetupAction = KotlinProjectSetupAction {
    val kotlinExtension = project.multiplatformExtension
    kotlinExtension.extensions.create(
        SwiftImportExtension.EXTENSION_NAME,
        SwiftImportExtension::class.java
    )
    val swiftPMImportExtension = kotlinExtension.getExtension<SwiftImportExtension>(
        SwiftImportExtension.EXTENSION_NAME
    )!!
    swiftPMImportExtension.spmDependencies.all { swiftPMDependency ->
        kotlinPropertiesProvider.enableCInteropCommonizationSetByExternalPlugin = true
        val syntheticImportProjectGenerationTask = project.locateOrRegisterTask<GenerateSyntheticImportProjectAndFetchPackages>(
            GenerateSyntheticImportProjectAndFetchPackages.TASK_NAME,
        ) {
            it.iosDeploymentVersion.set(swiftPMImportExtension.iosDeploymentVersion)
            it.macosDeploymentVersion.set(swiftPMImportExtension.macosDeploymentVersion)
            it.watchosDeploymentVersion.set(swiftPMImportExtension.watchosDeploymentVersion)
            it.tvosDeploymentVersion.set(swiftPMImportExtension.tvosDeploymentVersion)
        }
        syntheticImportProjectGenerationTask.configure {
            it.importedSpmModules.add(swiftPMDependency)
        }

        kotlinExtension.targets.matching {
            val targetSupportsSwiftPMImport = it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily
            targetSupportsSwiftPMImport
        }.all { target ->
            target as KotlinNativeTarget
            syntheticImportProjectGenerationTask.configure {
                it.konanTargets.add(target.konanTarget)
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
                it.dependsOn(syntheticImportProjectGenerationTask)
                it.xcodebuildPlatform.set(targetPlatform)
                it.xcodebuildSdk.set(targetSdk)
                it.swiftPMDependenciesCheckout.set(syntheticImportProjectGenerationTask.map { it.swiftPMDependenciesCheckout.get() })
                it.syntheticImportProjectRoot.set(syntheticImportProjectGenerationTask.map { it.syntheticImportProjectRoot.get() })
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

@DisableCachingByDefault(because = "...")
internal abstract class GenerateSyntheticImportProjectAndFetchPackages : DefaultTask() {

    @get:Input
    abstract val importedSpmModules: SetProperty<SwiftPMDependency>

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport")
    )

    // FIXME: Actually think about: "what do we want as a UTD check for the the packages checkout? The lock file?"
    // FIXME: We probably want this cache to be global
    @get:Internal
    val swiftPMDependenciesCheckout: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport/swiftPMCheckout")
    )

    @get:Internal
    protected val swiftPMDependenciesCheckoutLogs: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport/swiftPMCheckoutDD")
    )

    @get:OutputFile
    protected val syntheticImportProjectManifest
        get() = syntheticImportProjectRoot.file("Package.swift")

    // Force xcodebuild to call clang
    @get:OutputFile
    protected val syntheticImportProjectObjCSource
        get() = syntheticImportProjectRoot.file("Sources/${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}/${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.m")

    // Build system requires an include folder to be present and non-empty
    @get:OutputFile
    protected val syntheticImportProjectObjCHeaderFile
        get() = syntheticImportProjectRoot.file("Sources/${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}/include/${SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.h")

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

    @TaskAction
    fun generateSwiftPMSyntheticImportProjectAndFetchPackages() {
        syntheticImportProjectRoot.get().asFile.mkdirs()
        generatePackageManifest(importedSpmModules.get())
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

    private fun generatePackageManifest(
        dependencies: Set<SwiftPMDependency>,
    ) {
        val repoDependencies = dependencies.map {
            ".package(url: \"${it.repository}\", from: \"${it.fromVersion}\"),"
        }
        val targetDependencies = dependencies.flatMap { dep -> dep.products.map { it to dep.packageName } }.map {
            ".product(name: \"${it.first}\", package: \"${it.second}\"),"
        }

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
        syntheticImportProjectManifest.get().asFile.also {
            it.parentFile.mkdirs()
        }.writeText(
            buildString {
                appendLine("// swift-tools-version: 5.9")
                appendLine("import PackageDescription")
                appendLine("let package = Package(")
                appendLine("  name: \"$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME\",")
                appendLine("  platforms: [")
                platforms.forEach { appendLine("    $it")}
                appendLine("  ],")
                appendLine(
                    """
                        products: [
                            .library(
                                name: "$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME",
                                targets: ["$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME"],
                            ),
                        ],
                    """.replaceIndent("  ")
                )
                appendLine("  dependencies: [")
                repoDependencies.forEach { appendLine("    $it") }
                appendLine("  ],")
                appendLine("  targets: [")
                appendLine("    .target(")
                appendLine("      name: \"$SYNTHETIC_IMPORT_TARGET_MAGIC_NAME\",")
                appendLine("      dependencies: [")
                targetDependencies.forEach { appendLine("        $it") }
                appendLine("      ]")
                appendLine("    ),")
                appendLine("  ]")
                appendLine(")")
            }
        )

        // Generate ObjC sources specifically because the next CC-overriding step relies on passing a clang shim to dump compiler arguments
        syntheticImportProjectObjCSource.get().asFile.also {
            it.parentFile.mkdirs()
        }.writeText("")

        syntheticImportProjectObjCHeaderFile.get().asFile.also {
            it.parentFile.mkdirs()
        }.writeText("")
    }

    companion object {
        const val TASK_NAME = "generateSyntheticSwiftPMImportProject"
        const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME = "_internal_SwiftPMImport"
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

    @get:OutputDirectory
    protected val defFiles = xcodebuildSdk.flatMap { sdk ->
        project.layout.buildDirectory.dir("kotlin/swiftImportDefs/${sdk}")
    }

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Inject
    protected abstract val execOps: ExecOperations

    @get:Inject
    protected abstract val objects: ObjectFactory

    private val layout = project.layout

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
        val dd = projectRoot.resolve("dd_${xcodebuildSdk.get()}")
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
                    package = swiftPMImport
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

const val XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER = "-clonedSourcePackagesDirPath"