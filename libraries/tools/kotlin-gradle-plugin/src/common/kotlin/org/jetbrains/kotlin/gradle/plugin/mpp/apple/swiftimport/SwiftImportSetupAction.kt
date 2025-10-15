package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.getExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

internal val SwiftImportSetupAction = KotlinProjectSetupAction {
    val kotlinExtension = project.multiplatformExtension
    val extensionName = "spmImport"
    kotlinExtension.extensions.create<SwiftImportExtension>(
        extensionName,
        SwiftImportExtension::class.java
    )
//    kotlinExtension.addExtension(
//        extensionName,
//        SwiftImportExtension::class,
//    )
    val swiftPMImportExtension = kotlinExtension.getExtension<SwiftImportExtension>(extensionName)!!
    val swiftPMImportTask = tasks.register("swiftPMImport", SwiftPMImportTask::class.java) {
        it.iosDeploymentVersion.set(swiftPMImportExtension.iosDeploymentVersion)
    }
    swiftPMImportExtension.spmDependencies.all { dep ->
        swiftPMImportTask.configure {
            it.importedSpmModules.add(dep)
        }
    }

    val defFilesProvider = swiftPMImportTask.flatMap {
        it.defFiles
    }

    kotlinExtension.targets.matching {
        it is KotlinNativeTarget
    }.all { target ->
        target as KotlinNativeTarget
        swiftPMImportTask.configure {
            it.konanTargets.add(target.konanTarget)
        }
        target.compilations.getByName("main").cinterops.create("swiftPMImport").definitionFile.set(
            defFilesProvider.map { directory ->
                directory.file(("${target.konanTarget.name}.def"))
            }
        )
    }
}

internal abstract class SwiftPMImportTask : DefaultTask() {

    @get:Input
    abstract val importedSpmModules: SetProperty<SwiftPMDependency>

    @get:OutputDirectory
    val defFiles: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImportDefs")
    )

    @get:Internal
    val swiftPMTemporaries: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/swiftImport")
    )

    @get:Input
    abstract val konanTargets: SetProperty<KonanTarget>

    @get:Input
    abstract val iosDeploymentVersion: Property<String>

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val objects: ObjectFactory

    @TaskAction
    fun importSwiftPMDependencies() {
        if (importedSpmModules.get().size == 0) {
            konanTargets.get().forEach {
                val defFilesRoot = defFiles.get().asFile
                defFilesRoot.resolve("${it.name}.def").writeText(
                    """
                    language = Objective-C
                    ---
                """.trimIndent()
                )
            }
            return
        }
        val temporaries = swiftPMTemporaries.get().asFile
        temporaries.mkdirs()
        generatePackageManifest(temporaries, importedSpmModules.get())

        val dd = temporaries.resolve("dd")

        val clangArgsDumpScript = temporaries.resolve("clangDump.sh")
        clangArgsDumpScript.writeText(searchPathsDumpScript())
        clangArgsDumpScript.setExecutable(true)
        val clangArgsDump = temporaries.resolve("clang_args_dump")
        if (clangArgsDump.exists()) {
            clangArgsDump.deleteRecursively()
        }
        clangArgsDump.mkdirs()

        val target = konanTargets.get().first()
        // FIXME: Do we need to actually build for all targets at this point? Check what happens with macros
//        konanTargets.get().stream().limit(1).forEach { target ->
//        }
        val platform = target.appleTarget.applePlatform
        execOps.exec {
            it.workingDir(temporaries)
            it.commandLine(
                "xcodebuild", "build",
                "-scheme", "_internal_SwiftPMImport",
                "-destination", "generic/platform=${platform}",
                "-derivedDataPath", dd.path,
                "IPHONEOS_DEPLOYMENT_TARGET=${'$'}RECOMMENDED_IPHONEOS_DEPLOYMENT_TARGET",
                "MACOSX_DEPLOYMENT_TARGET=${'$'}RECOMMENDED_MACOSX_DEPLOYMENT_TARGET",
                "TVOS_DEPLOYMENT_TARGET=${'$'}RECOMMENDED_TVOS_DEPLOYMENT_TARGET",
                "WATCHOS_DEPLOYMENT_TARGET=${'$'}RECOMMENDED_WATCHOS_DEPLOYMENT_TARGET",
                "CC=${clangArgsDumpScript.path}",
                // FIXME: ???
                "ARCHS=arm64",
            )
            it.environment(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV, clangArgsDump)
        }

        val searchPathStrings = mutableSetOf<String>()
        val frameworkSearchPaths = mutableSetOf<File>()
        val moduleSearchPaths = mutableSetOf<File>()
        val targetClangCall = clangArgsDump.listFiles().filter {
            it.isFile
        }.single {
            val clangArgs = it.readLines().single()
            "-fmodule-name=_internal_SwiftPMImport" in clangArgs
        }
        targetClangCall.readLines().single().split(";").forEach { arg ->
            if (arg.startsWith("-F")) {
                frameworkSearchPaths.add(
                    File(arg.substring(2))
                )
                searchPathStrings.add(arg)
            }
            if (arg.startsWith("-I")) {
                moduleSearchPaths.add(
                    File(arg.substring(2))
                )
                searchPathStrings.add(arg)
            }
            if (arg.startsWith("-fmodule-map-file=")) {
                searchPathStrings.add(arg)
            }
        }
//        val searchPaths = clangArgsDump.useLines {
//            // FIXME: Use an Xcode proj instead and override CC for this project only
//            val finalClangCall = it.filter { it.isNotEmpty() && "-fmodule-name=_internal_SwiftPMImport" in it }.single()
//            finalClangCall.split(";").forEach {
//                if (it.startsWith("-F")) {}
//                frameworkSearchPaths.add(
//
//                )
//            }.filter { it.startsWith("-I") || it.startsWith("-F") }
//            searchPaths
//        }
        fun extractModuleNames(moduleMap: File): List<String> {
            val moduleNames = mutableListOf<String>()
            moduleMap.readLines().forEach { line ->
                if ("module " in line) {
                    val moduleName = line.split(" ").dropWhile {
                        it != "module"
                    }.drop(1).take(1).single()
                    moduleNames.add(moduleName)
                }
            }
            return moduleNames
        }

        val availableModuleNames = mutableListOf<String>()
        moduleSearchPaths.forEach { searchPath ->
            if (searchPath.exists()) {
                searchPath.listFiles().filter {
                    it.name == "module.modulemap"
                }.forEach {
                    availableModuleNames.addAll(extractModuleNames(it))
                }
            }
        }
        frameworkSearchPaths.forEach { searchPath ->
            if (searchPath.exists()) {
                searchPath.listFiles().filter {
                    it.extension == "framework"
                }.forEach {
                    it.walk().forEach { file ->
                        if (file.extension == "modulemap") {
                            availableModuleNames.addAll(extractModuleNames(file))
                        }
                    }
                }
            }
        }

        // FIXME: Validate that in "modules" we have something that is actually an available module
        // println(availableModuleNames)
//        val sdkName = target.appleTarget.sdk
//        val moduleMapsAndSwiftHeaders = dd.resolve("Build/Intermediates.noindex/GeneratedModuleMaps-${sdkName}")

        // val moduleMaps = moduleMapsAndSwiftHeaders.listFiles().filter { it.extension == "modulemap" && "_internal_SwiftPMImport" !in it.name }
        // FIXME: This is incorrect, look at modules inside the modulemap instead?
//        val modules = moduleMaps.map {
//            it.useLines { lines ->
//                lines.first().split(" ")[1]
//            }
//        }

//        val sps = hackImplicitSearchPath(
//            temporaries = temporaries,
//            modulemaps = moduleMapsAndSwiftHeaders,
//        ).joinToString(" ") {
//            "\"-I${it.path}\""
//        }

        val defFileSeachPaths = searchPathStrings.joinToString(" ") { "\"${it}\"" }
        val modules = importedSpmModules.get().flatMap {
            it.cinteropTargets
        }

        // Stop -Swift.h headers from pushing duplicate typedefs and exploding the cinterop
        val workaroundKT81695 = "-DSWIFT_TYPEDEFS"

        konanTargets.get().forEach {
            val defFilesRoot = defFiles.get().asFile
            defFilesRoot.resolve("${it.name}.def").writeText(
                """
                    language = Objective-C
                    modules = ${modules.joinToString(" ")}
                    compilerOpts = ${workaroundKT81695} -fmodules ${defFileSeachPaths}
                    package = swiftPMImport
                """.trimIndent()
            )
        }
    }

    private fun generatePackageManifest(
        temporaries: File,
        dependencies: Set<SwiftPMDependency>,
    ) {
        val repoDependencies = dependencies.joinToString("\n") {
            ".package(url: \"${it.repository}\", exact: \"${it.version}\"),"
        }
        val targetDependencies = dependencies.flatMap { dep -> dep.products.map { it to dep.packageName } }.joinToString("\n") {
            ".product(name: \"${it.first}\", package: \"${it.second}\"),"
        }
        // FIXME: Make the platform version configurable
        temporaries.resolve("Package.swift").writeText(
            """
                // swift-tools-version: 5.9
        
        import PackageDescription
        let package = Package(
            name: "_internal_SwiftPMImport",
            platforms: [
                .iOS("${iosDeploymentVersion.get()}"),
            ],
            products: [
                .library(
                    name: "_internal_SwiftPMImport",
                    targets: ["_internal_SwiftPMImport"]
                ),
            ],
            dependencies: [
                $repoDependencies
            ],
            targets: [
                .target(
                    name: "_internal_SwiftPMImport",
                    dependencies: [
                        $targetDependencies
                    ]
                )
            ]
        )
            """.trimIndent()
        )

        val emptySource = temporaries.resolve("Sources/_internal_SwiftPMImport/_internal_SwiftPMImport.m")
        emptySource.parentFile.mkdirs()
        emptySource.writeText("")

        val emptyHeader = temporaries.resolve("Sources/_internal_SwiftPMImport/include/_internal_SwiftPMImport.h")
        emptyHeader.parentFile.mkdirs()
        emptyHeader.writeText("")
    }

    // Why does cinterop not accept explicit -fmodule-map-file=<File>?
    private fun hackImplicitSearchPath(
        temporaries: File,
        modulemaps: File,
    ): List<File> {
        val implicitSearchPaths = temporaries.resolve("hackImplicitSearchPath")
        implicitSearchPaths.mkdirs()
        val outgoingImplicitPaths = mutableListOf<File>()

        modulemaps.listFiles().filter { it.extension == "modulemap" && "_internal_SwiftPMImport" !in it.name }.forEach {
            val spd = implicitSearchPaths.resolve("${it.nameWithoutExtension}")
            spd.mkdirs()
            it.copyTo(spd.resolve("module.modulemap"), true)
            val swiftBridge =it.parentFile.resolve("${it.nameWithoutExtension}-Swift.h")
            if (swiftBridge.exists()) {
                swiftBridge.copyTo(spd.resolve(swiftBridge.name), true)
            }
            outgoingImplicitPaths.add(spd)
        }
        return outgoingImplicitPaths
    }

    private fun searchPathsDumpScript() = """
        #!/bin/bash

        DUMP_FILE="${'$'}{${KOTLIN_CLANG_ARGS_DUMP_FILE_ENV}}/${'$'}(/usr/bin/uuidgen)"
        for arg in "$@"
        do
           echo -n "${'$'}arg" >> "${'$'}{DUMP_FILE}"
           echo -n ";" >> "${'$'}{DUMP_FILE}"
        done

        clang "$@"
    """.trimIndent()

    companion object {
        const val KOTLIN_CLANG_ARGS_DUMP_FILE_ENV = "KOTLIN_CLANG_ARGS_DUMP_FILE"
    }
}