/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

internal class CompilerPluginData(
    val files: FileCollection,
    val options: CompilerPluginOptions,
)

internal class SharedCompilationData(
    val manifestFile: File,
    val refinesPaths: FileCollection,
)

internal fun buildKotlinNativeBinaryLinkerArgs(
    outFile: File,
    optimized: Boolean,
    debuggable: Boolean,
    target: KonanTarget,
    outputKind: CompilerOutputKind,
    libraries: List<File>,
    friendModules: List<File>,

    toolOptions: KotlinCommonCompilerToolOptions,
    compilerPlugins: List<CompilerPluginData>,

    processTests: Boolean,
    entryPoint: String?,
    embedBitcode: BitcodeEmbeddingMode,
    linkerOpts: List<String>,
    binaryOptions: Map<String, String>,
    isStaticFramework: Boolean,
    exportLibraries: List<File>,
    includeLibraries: List<File>,
    additionalOptions: Collection<String>,
): List<String> = mutableListOf<String>().apply {
    addAll(buildKotlinNativeMainArgs(outFile, optimized, debuggable, target, outputKind, libraries))
    addAll(additionalOptions)

    addKey("-tr", processTests)
    addArgIfNotNull("-entry", entryPoint)
    when (embedBitcode) {
        BitcodeEmbeddingMode.MARKER -> add("-Xembed-bitcode-marker")
        BitcodeEmbeddingMode.BITCODE -> add("-Xembed-bitcode")
        else -> Unit
    }
    linkerOpts.forEach { addArg("-linker-option", it) }
    binaryOptions.forEach { (name, value) -> add("-Xbinary=$name=$value") }
    addKey("-Xstatic-framework", isStaticFramework)

    addAll(buildKotlinNativeCommonArgs(toolOptions, compilerPlugins))

    exportLibraries.forEach { add("-Xexport-library=${it.absolutePath}") }
    includeLibraries.forEach { add("-Xinclude=${it.absolutePath}") }

    if (friendModules.isNotEmpty()) {
        addArg("-friend-modules", friendModules.joinToString(File.pathSeparator) { it.absolutePath })
    }
}

private fun buildKotlinNativeMainArgs(
    outFile: File,
    optimized: Boolean,
    debuggable: Boolean,
    target: KonanTarget,
    outputKind: CompilerOutputKind,
    libraries: List<File>,
): List<String> = mutableListOf<String>().apply {
    addKey("-opt", optimized)
    addKey("-g", debuggable)
    addKey("-ea", debuggable)
    addArg("-target", target.name)
    addArg("-p", outputKind.name.toLowerCaseAsciiOnly())
    addArg("-o", outFile.absolutePath)
    libraries.forEach { addArg("-l", it.absolutePath) }
}

private fun buildKotlinNativeCommonArgs(
    toolOptions: KotlinCommonCompilerToolOptions,
    compilerPlugins: List<CompilerPluginData>,
): List<String> = mutableListOf<String>().apply {
    add("-Xmulti-platform")
    addKey("-no-endorsed-libs", true)

    compilerPlugins.forEach { plugin ->
        plugin.files.map { it.canonicalPath }.sorted().forEach { add("-Xplugin=$it") }
        addArgs("-P", plugin.options.arguments)
    }

    addKey("-Werror", toolOptions.allWarningsAsErrors.get())
    addKey("-nowarn", toolOptions.suppressWarnings.get())
    addKey("-verbose", toolOptions.verbose.get())

    addAll(toolOptions.freeCompilerArgs.get())
}

private fun MutableList<String>.addArg(parameter: String, value: String) {
    add(parameter)
    add(value)
}

private fun MutableList<String>.addArgs(parameter: String, values: Iterable<String>) {
    values.forEach {
        addArg(parameter, it)
    }
}

private fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
    if (value != null) {
        addArg(parameter, value)
    }
}

private fun MutableList<String>.addKey(key: String, enabled: Boolean) {
    if (enabled) {
        add(key)
    }
}
