/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import java.io.File

private val MODULE_NAME_REGEX = Regex("\\bmodule ([A-Za-z0-9_.]+) ")

internal object XcodebuildDefFileUtils {

    const val KOTLIN_CLANG_ARGS_DUMP_FILE_ENV = "KOTLIN_CLANG_ARGS_DUMP_FILE"
    const val KOTLIN_LD_ARGS_DUMP_FILE_ENV = "KOTLIN_LD_ARGS_DUMP_FILE"
    const val DUMP_FILE_ARGS_SEPARATOR = ";"

    fun defFilesRelativeDir(sdk: String) = "kotlin/swiftImportDefs/$sdk"
    fun ldDumpRelativeDir(sdk: String) = "kotlin/swiftImportLdDump/$sdk"
    fun clangDumpRelativeDir(sdk: String) = "kotlin/swiftImportClangDump/$sdk"
    const val SYNTHETIC_IMPORT_DD_DIR = "kotlin/swiftImportDd"

    fun defFileName(architecture: AppleArchitecture) = "${architecture.xcodebuildArch}.def"
    fun ldFileName(architecture: AppleArchitecture) = "${architecture.xcodebuildArch}.ld"
    fun frameworkLdFileName(architecture: AppleArchitecture) = "${architecture.xcodebuildArch}.framework.ld"
    fun ldFingerprintFileName(architecture: AppleArchitecture) = "${architecture.xcodebuildArch}.timestamp.ld"
    fun frameworkSearchpathFileName(architecture: AppleArchitecture) = "${architecture.xcodebuildArch}_framework_search_paths"
    fun librarySearchpathFileName(architecture: AppleArchitecture) = "${architecture.xcodebuildArch}_library_search_paths"

    data class ParsedClangCall(
        val cinteropClangArgs: List<String>,
        val compileTimeFrameworkSearchPaths: Set<String>,
        val includeSearchPaths: Set<String>,
        val explicitModuleMaps: Set<String>,
    )

    data class ParsedLdCall(
        val ldArgs: List<String>,
        val frameworkLdArgs: List<String>,
        val linkTimeFrameworkSearchPaths: Set<String>,
        val librarySearchPaths: Set<String>,
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

    fun parseLdCall(architectureSpecificProductLdCall: File): ParsedLdCall {
        val resplitLdCall = architectureSpecificProductLdCall.readLines().single().split(DUMP_FILE_ARGS_SEPARATOR)
        val ldArgs = mutableListOf<String>()
        val filelist = mutableListOf<String>()
        val kotlinDylibProduct = mutableListOf<String>()
        val linkTimeFrameworkSearchPaths = mutableSetOf<String>()
        val librarySearchPaths = mutableSetOf<String>()

        resplitLdCall.forEachIndexed { index, arg ->
            if (arg == "-filelist") {
                filelist.addAll(listOf(arg, resplitLdCall[index + 1]))
            }
            if (
                arg == "-framework"
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
                    if (arg.endsWith("/" + GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB)) {
                        kotlinDylibProduct.add(arg)
                    } else {
                        ldArgs.add(arg)
                    }
                    linkTimeFrameworkSearchPaths.add(
                        File(arg).parentFile.parentFile.path
                    )
                }
            }
        }

        return ParsedLdCall(
            ldArgs = ldArgs + filelist,
            frameworkLdArgs = kotlinDylibProduct + ldArgs,
            linkTimeFrameworkSearchPaths = linkTimeFrameworkSearchPaths,
            librarySearchPaths = librarySearchPaths,
        )
    }

    fun discoverClangModules(parsedClangCall: ParsedClangCall): Set<String> {
        fun inferModuleName(modulemap: File): String? = MODULE_NAME_REGEX.find(modulemap.readText())?.let {
            it.groups[1]?.value
        }

        /**
         * FIXME: KT-84809 Discovery logic will break on incremental runs as it will discover stale modules (same issue with Xcode)
         */
        val implicitlyDiscoveredModules = mutableSetOf<String>()
        parsedClangCall.compileTimeFrameworkSearchPaths.map { File(it) }.filter { it.exists() }.forEach {
            implicitlyDiscoveredModules.addAll(
                it.listFilesOrEmpty().filter { child ->
                    child.extension == "framework"
                }.filter { framework ->
                    val children = framework.listFilesOrEmpty()
                    val hasModules = children.any { child -> child.name == "Modules" }
                    // Some libraries like GoogleAppMeasurement have a modulemap with dangling header references
                    val hasHeaders = children.any { child -> child.name == "Headers" }
                    hasModules && hasHeaders
                }.map { framework ->
                    framework.nameWithoutExtension
                }
            )
        }
        parsedClangCall.includeSearchPaths.map { File(it) }.filter { it.exists() }.forEach { searchPath ->
            searchPath.listFilesOrEmpty().forEach { searchPathFile ->
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
                    searchPathFile.listFilesOrEmpty().filter {
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

    fun writeDefFile(
        parsedClangCall: ParsedClangCall,
        clangModules: Set<String>,
        architecture: AppleArchitecture,
        defFilesDir: File,
        cinteropNamespace: String,
        discoverModulesImplicitly: Boolean,
    ) {
        val defFileSearchPaths = parsedClangCall.cinteropClangArgs.joinToString(" ") { "\"${it}\"" }
        val modules = clangModules.joinToString(" ") { "\"${it}\"" }

        val defFile = defFilesDir.resolve("${architecture.xcodebuildArch}.def")
        defFile.writeText(
            buildString {
                appendLine("language = Objective-C")
                appendLine("compilerOpts = -fmodules $defFileSearchPaths")
                appendLine("package = $cinteropNamespace")
                if (modules.isNotEmpty()) {
                    appendLine("modules = $modules")
                }
                val invalidateDownstreamCinterops = System.currentTimeMillis()
                if (discoverModulesImplicitly) {
                    appendLine("skipNonImportableModules = true")
                }
                appendLine(
                    """
                        ---
                        // $invalidateDownstreamCinterops
                    """.trimIndent()
                )
            }
        )
    }

    fun clangArgsDumpScript(): String = argsDumpScript(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV)
    fun ldArgsDumpScript(): String = argsDumpScript(KOTLIN_LD_ARGS_DUMP_FILE_ENV)

    private fun argsDumpScript(
        dumpPathEnv: String,
    ) = """
        #!/bin/bash

        DUMP_FILE="${'$'}{${dumpPathEnv}}/${'$'}(/usr/bin/uuidgen)"
        for arg in "$@"
        do
           echo -n "${'$'}arg" >> "${'$'}{DUMP_FILE}"
           echo -n "$DUMP_FILE_ARGS_SEPARATOR" >> "${'$'}{DUMP_FILE}"
        done

        clang "$@"
    """.trimIndent()
}
