/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject
import kotlin.collections.joinToString

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class ConvertSyntheticSwiftPMImportProjectIntoDefFile : DefaultTask() {

    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val clangModules: SetProperty<String>

    @get:Input
    abstract val discoverModulesImplicitly: Property<Boolean>

    @get:Input
    abstract val hasSwiftPMDependencies: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val localPackageSources get() = filesToTrackFromLocalPackages.map { it.asFile.readLines().filter { it.isNotEmpty() }.map {
        File(
            it
        )
    } }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    private val layout = project.layout

    @get:OutputDirectory
    protected val defFiles = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir("kotlin/swiftImportDefs/${sdk}")
    }

    @get:OutputDirectory
    protected val ldDump = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir("kotlin/swiftImportLdDump/${sdk}")
    }

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Internal
    val syntheticImportDd = layout.buildDirectory.dir("kotlin/swiftImportDd")

    @get:Inject
    protected abstract val execOps: ExecOperations

    @get:Inject
    protected abstract val objects: ObjectFactory

    private val cinteropNamespace = listOf(
        "swiftPMImport",
        project.group.toString(),
        if (project.path == ":") project.name else project.path.drop(1)
    ).filter {
        it.isNotEmpty()
    }.joinToString(".") {
        it.replace(Regex("[^a-zA-Z0-9_.]"), ".")
    }.replace(Regex("\\.{2,}"), ".")  // Replace multiple consecutive dots with single dot
     .trim('.')  // Remove leading/trailing dots

    @TaskAction
    fun generateDefFiles() {
        if (!hasSwiftPMDependencies.get()) {
            architectures.get().forEach { architecture ->
                /**
                 * Stub out all outputs if we have no SwiftPM dependencies
                 */
                defFilePath(architecture).getFile().writeText(
                    """
                        language = Objective-C
                        package = $cinteropNamespace
                    """.trimIndent()
                )
                ldFilePath(architecture).getFile().writeText("\n")
                ldFileFingerprintPath(architecture).getFile().writeText("0")
                frameworkSearchpathFilePath(architecture).getFile().writeText("\n")
                librarySearchpathFilePath(architecture).getFile().writeText("\n")
            }
            return
        }

        val dumpIntermediates = xcodebuildSdk.flatMap { sdk ->
            layout.buildDirectory.dir("kotlin/swiftImportClangDump/${sdk}")
        }.get().asFile.also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdirs()
        }

        val clangArgsDumpScript = dumpIntermediates.resolve("clangDump.sh")
        clangArgsDumpScript.writeText(clangArgsDumpScript())
        clangArgsDumpScript.setExecutable(true)
        val clangArgsDump = dumpIntermediates.resolve("clang_args_dump")
        clangArgsDump.mkdirs()

        val ldArgsDumpScript = dumpIntermediates.resolve("ldDump.sh")
        ldArgsDumpScript.writeText(ldArgsDumpScript())
        ldArgsDumpScript.setExecutable(true)
        val ldArgsDump = dumpIntermediates.resolve("ld_args_dump")
        ldArgsDump.mkdirs()

        val targetArchitectures = architectures.get().map {
            it.xcodebuildArch
        }

        val projectRoot = syntheticImportProjectRoot.get().asFile

        /**
         * For some reason reusing dd in parallel xcodebuild calls explodes something in the build system, so we need a
         * separate dd per "-destination" platform that can run in parallel
         */
        val dd = syntheticImportDd.get().asFile.resolve("dd_${xcodebuildSdk.get()}")

        // FIXME: KT-84809 - This is not great, but we can't remove entire DD on incremental runs
        val forceClangToReexecute = dd.resolve("Build/Intermediates.noindex/${GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.build")
        if (forceClangToReexecute.exists()) {
            forceClangToReexecute.deleteRecursively()
        }

        execOps.exec { exec ->
            exec.workingDir(projectRoot)
            exec.commandLine(
                "xcodebuild", "build",
                "-scheme", GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                "-destination", "generic/platform=${xcodebuildPlatform.get()}",
                "-derivedDataPath", dd.path,
                // FIXME: KT-83863 We probably want to force xcodebuild to only to resolution during the "fetch" stage and then only build here. Does this parameter help us?
                // "-disableAutomaticPackageResolution",
                FetchSyntheticImportProjectPackages.XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER, swiftPMDependenciesCheckout.get().asFile.path,
                "CC=${clangArgsDumpScript.path}",
                "LD=${ldArgsDumpScript.path}",
                "ARCHS=${targetArchitectures.joinToString(" ")}",
                // Avoid codesigning
                "CODE_SIGN_IDENTITY=",
                // Avoid emitting indexes
                "COMPILER_INDEX_STORE_ENABLE=NO",
                "SWIFT_INDEX_STORE_ENABLE=NO",
                // This will force the .dylib to be created instead of the framework. Do we actually want to account for this?
                // "-IDEPackageSupportCreateDylibsForDynamicProducts=YES"
            )
            exec.environment(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV, clangArgsDump)
            exec.environment(KOTLIN_LD_ARGS_DUMP_FILE_ENV, ldArgsDump)

            val environmentToFilter = listOf(
                "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES",
            ) + AppleSdk.xcodeEnvironmentDebugDylibVars
            environmentToFilter.forEach {
                if (exec.environment.containsKey(it)) {
                    exec.environment.remove(it)
                }
            }
            exec.environment.keys.filter {
                // ScanDependencies explode with duplicate modules because it reads this env for some reason
                it.startsWith("OTHER_")
                        // Also some asset catalogs utility explodes
                        || it.startsWith("ASSETCATALOG_")
            }.forEach {
                exec.environment.remove(it)
            }
        }

        architectures.get().forEach { architecture ->
            val clangArchitecture = architecture.clangArch
            val architectureSpecificProductClangCalls = mutableListOf<File>()

            clangArgsDump.listFiles().filter {
                it.isFile
            }.forEach {
                val clangArgs = it.readLines().single()
                val isArchitectureSpecificProductClangCall = "-fmodule-name=${GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}" in clangArgs
                        && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in clangArgs
                if (isArchitectureSpecificProductClangCall) {
                    architectureSpecificProductClangCalls.add(it)
                }
            }

            val parsedClangCall = parseClangCall(architectureSpecificProductClangCalls.single())

            val clangModules = if (discoverModulesImplicitly.get()) {
                discoverClangModules(parsedClangCall)
            } else clangModules.get()

            writeDefFile(parsedClangCall, clangModules, architecture)

            val architectureSpecificProductLdCalls = ldArgsDump.listFiles().filter {
                it.isFile
            }.filter {
                // This will actually be a clang call
                val ldArgs = it.readLines().single()
                ("@rpath/lib${GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.dylib" in ldArgs || "@rpath/${GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME}.framework" in ldArgs)
                        && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in ldArgs
            }

            val parsedLdCall = parseLdCall(architectureSpecificProductLdCalls.single())

            ldFilePath(architecture).getFile()
                .writeText(parsedLdCall.ldArgs.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            ldFileFingerprintPath(architecture).getFile()
                .writeText(System.currentTimeMillis().toString())
            frameworkSearchpathFilePath(architecture).getFile()
                .writeText(parsedLdCall.linkTimeFrameworkSearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            librarySearchpathFilePath(architecture).getFile()
                .writeText(parsedLdCall.librarySearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR))
        }
    }

    fun writeDefFile(
        parsedClangCall: ParsedClangCall,
        clangModules: Set<String>,
        architecture: AppleArchitecture
    ) {
        val defFileSearchPaths = parsedClangCall.cinteropClangArgs.joinToString(" ") { "\"${it}\"" }
        val modules = clangModules.joinToString(" ") { "\"${it}\"" }

        val defFilePath = defFilePath(architecture)
        defFilePath.getFile().writeText(
            buildString {
                appendLine("language = Objective-C")
                appendLine("compilerOpts = -fmodules $defFileSearchPaths")
                appendLine("package = $cinteropNamespace")
                if (modules.isNotEmpty()) {
                    appendLine("modules = $modules")
                }
                val invalidateDownstreamCinterops = System.currentTimeMillis()
                if (discoverModulesImplicitly.get()) {
                    appendLine("skipNonImportableModules = true")
                }
                appendLine("""
                        ---
                        // $invalidateDownstreamCinterops
                    """.trimIndent())
            }
        )
    }

    data class ParsedLdCall(
        val ldArgs: List<String>,
        val linkTimeFrameworkSearchPaths: Set<String>,
        val librarySearchPaths: Set<String>,
    )

    fun parseLdCall(architectureSpecificProductLdCall: File): ParsedLdCall {
        val resplitLdCall = architectureSpecificProductLdCall.readLines().single().split(DUMP_FILE_ARGS_SEPARATOR)
        val ldArgs = mutableListOf<String>()
        val linkTimeFrameworkSearchPaths = mutableSetOf<String>()
        val librarySearchPaths = mutableSetOf<String>()

        resplitLdCall.forEachIndexed { index, arg ->
            if (
                // Most linkage dependencies are passed in the filelist
                arg == "-filelist"
                || arg == "-framework"
                || (arg.startsWith("-") && arg.endsWith("_framework"))
            ) {
                ldArgs.addAll(listOf(arg, resplitLdCall[index + 1]))
            }
            if (arg.startsWith("-l")) {
                ldArgs.add(arg)
            }
            if (arg.startsWith("-F/")) {
                ldArgs.add(arg)
                linkTimeFrameworkSearchPaths.add(arg.substring(2))
            }
            if (arg.startsWith("-L/")) {
                ldArgs.add(arg)
                librarySearchPaths.add(arg.substring(2))
            }

            // Unpacked XCFramework slices are passed as a CLI path
            if (arg.startsWith("/")) {
                if (arg.endsWith(".a")) {
                    ldArgs.add(arg)
                }
                if (arg.endsWith(".dylib")) {
                    ldArgs.add(arg)
                    librarySearchPaths.add((File(arg).parentFile.path))
                }
                if (".framework/" in arg) {
                    ldArgs.add(arg)
                    linkTimeFrameworkSearchPaths.add(
                        File(arg).parentFile.parentFile.path
                    )
                }
            }
        }

        return ParsedLdCall(
            ldArgs = ldArgs,
            linkTimeFrameworkSearchPaths = linkTimeFrameworkSearchPaths,
            librarySearchPaths = librarySearchPaths,
        )
    }

    fun discoverClangModules(parsedClangCall: ParsedClangCall): Set<String> {
        val moduleName = Regex("\\bmodule ([A-Za-z0-9_.]+) ")
        fun inferModuleName(modulemap: File): String? = moduleName.find(modulemap.readText())?.let {
            it.groups[1]?.value
        }

        /**
         * FIXME: KT-84809 Discovery logic will break on incremental runs as it will discover stale modules (same issue with Xcode)
         */
        val implicitlyDiscoveredModules = mutableSetOf<String>()
        parsedClangCall.compileTimeFrameworkSearchPaths.map { File(it) }.filter { it.exists() }.forEach {
            implicitlyDiscoveredModules.addAll(
                it.listFiles().filter {
                    it.extension == "framework"
                }.filter { framework ->
                    val hasModules = framework.listFiles().any { it.name == "Modules" }
                    // Some libraries like GoogleAppMeasurement have a modulemap with dangling header references
                    val hasHeaders = framework.listFiles().any { it.name == "Headers" }
                    hasModules && hasHeaders
                }.map { framework ->
                    framework.nameWithoutExtension
                }
            )
        }
        parsedClangCall.includeSearchPaths.map { File(it) }.filter { it.exists() }.forEach { searchPath ->
            searchPath.listFiles().forEach { searchPathFile ->
                if (searchPathFile.name == "module.modulemap") {
                    val module = inferModuleName(searchPathFile)
                    if (module != null) {
                        implicitlyDiscoveredModules.add(module)
                    }
                }
                // Also discover modules in the form
                // -I/search/path
                // /search/path/ModuleName/module.modulemap
                // E.g. GoogleMaps
                if (searchPathFile.isDirectory) {
                    searchPathFile.listFiles().filter {
                        it.name == "module.modulemap"
                    }.forEach { subsearchPathFile ->
                        val module = inferModuleName(subsearchPathFile)
                        // The module must be equal to the directory name, same as with frameworks
                        if (module != null && module == searchPathFile.name) {
                            implicitlyDiscoveredModules.add(module)
                        }
                    }
                }
            }
        }
        implicitlyDiscoveredModules.addAll(
            parsedClangCall.explicitModuleMaps.mapNotNull {
                inferModuleName(File(it))
            }
        )
        return implicitlyDiscoveredModules
    }

    data class ParsedClangCall(
        val cinteropClangArgs: List<String>,
        val compileTimeFrameworkSearchPaths: Set<String>,
        val includeSearchPaths: Set<String>,
        val explicitModuleMaps: Set<String>,
    )

    fun parseClangCall(architectureSpecificProductClangCall: File): ParsedClangCall {
        val cinteropClangArgs = mutableListOf<String>()
        val compileTimeFrameworkSearchPaths = mutableSetOf<String>()
        val includeSearchPaths = mutableSetOf<String>()
        val explicitModuleMaps = mutableSetOf<String>()
        architectureSpecificProductClangCall.readLines().single().split(DUMP_FILE_ARGS_SEPARATOR).forEach { arg ->
            val frameworkSearchPathArg = "-F"
            if (arg.startsWith(frameworkSearchPathArg)) {
                cinteropClangArgs.add(arg)
                compileTimeFrameworkSearchPaths.add(arg.substring(frameworkSearchPathArg.length))
            }
            val includeSearchPathArg = "-I"
            if (arg.startsWith(includeSearchPathArg)) {
                cinteropClangArgs.add(arg)
                includeSearchPaths.add(arg.substring(includeSearchPathArg.length))
            }
            val explicitModuleMapArg = "-fmodule-map-file="
            if (arg.startsWith(explicitModuleMapArg)) {
                cinteropClangArgs.add(arg)
                explicitModuleMaps.add(arg.substring(explicitModuleMapArg.length))
            }
        }
        return ParsedClangCall(
            cinteropClangArgs = cinteropClangArgs,
            compileTimeFrameworkSearchPaths = compileTimeFrameworkSearchPaths,
            includeSearchPaths = includeSearchPaths,
            explicitModuleMaps = explicitModuleMaps,
        )
    }

    fun defFilePath(architecture: AppleArchitecture) = defFiles.map { it.file("${architecture.xcodebuildArch}.def") }
    fun ldFilePath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}.ld") }
    fun ldFileFingerprintPath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}.timestamp.ld") }
    fun frameworkSearchpathFilePath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}_framework_search_paths") }
    fun librarySearchpathFilePath(architecture: AppleArchitecture) = ldDump.map { it.file("${architecture.xcodebuildArch}_library_search_paths") }

    private fun clangArgsDumpScript() = argsDumpScript("clang", KOTLIN_CLANG_ARGS_DUMP_FILE_ENV)
    private fun ldArgsDumpScript() = argsDumpScript("clang", KOTLIN_LD_ARGS_DUMP_FILE_ENV)

    private fun argsDumpScript(
        targetCli: String,
        dumpPathEnv: String,
    ) = """
        #!/bin/bash

        DUMP_FILE="${'$'}{${dumpPathEnv}}/${'$'}(/usr/bin/uuidgen)"
        for arg in "$@"
        do
           echo -n "${'$'}arg" >> "${'$'}{DUMP_FILE}"
           echo -n "$DUMP_FILE_ARGS_SEPARATOR" >> "${'$'}{DUMP_FILE}"
        done

        ${targetCli} "$@"
    """.trimIndent()

    companion object {
        const val KOTLIN_CLANG_ARGS_DUMP_FILE_ENV = "KOTLIN_CLANG_ARGS_DUMP_FILE"
        const val KOTLIN_LD_ARGS_DUMP_FILE_ENV = "KOTLIN_LD_ARGS_DUMP_FILE"
        const val DUMP_FILE_ARGS_SEPARATOR = ";"
        const val TASK_NAME = "convertSyntheticImportProjectIntoDefFile"
    }

}