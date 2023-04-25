/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.usesK2
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.fragmentSourcesCompilerArgs
import org.jetbrains.kotlin.gradle.tasks.fragmentsCompilerArgs
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.project.model.LanguageSettings
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

internal class CompilerPluginData(
    val files: FileCollection,
    val options: CompilerPluginOptions
)

internal class SharedCompilationData(
    val manifestFile: File,
    val isAllowCommonizer: Boolean,
    val refinesPaths: FileCollection
)

@Suppress("UNUSED_PARAMETER")
internal fun buildKotlinNativeKlibCompilerArgs(
    outFile: File,
    optimized: Boolean,
    debuggable: Boolean,
    target: KonanTarget,
    libraries: List<File>,

    languageSettings: LanguageSettings,
    compilerOptions: KotlinNativeCompilerOptions,
    compilerPlugins: List<CompilerPluginData>,

    shortModuleName: String,
    friendModule: FileCollection,
    libraryVersion: String,
    sharedCompilationData: SharedCompilationData?,
    source: FileTree,
    commonSourcesTree: FileTree,
    k2MultiplatformCompilationData: K2MultiplatformStructure
): List<String> = mutableListOf<String>().apply {
    addAll(buildKotlinNativeMainArgs(outFile, optimized, debuggable, target, CompilerOutputKind.LIBRARY, libraries))

    sharedCompilationData?.let {
        add("-Xexpect-actual-linker")
        add("-Xmetadata-klib")
        addArg("-manifest", sharedCompilationData.manifestFile.absolutePath)
        addKey("-no-default-libs", sharedCompilationData.isAllowCommonizer)
    }

    // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
    addArg("-module-name", compilerOptions.moduleName.get())
    add("-Xshort-module-name=$shortModuleName")

    val friends = friendModule.files
    if (friends.isNotEmpty()) {
        addArg("-friend-modules", friends.joinToString(File.pathSeparator) { it.absolutePath })
    }

    // TODO: uncomment after advancing bootstrap.
    //add("-library-version=libraryVersion")

    if (sharedCompilationData != null) {
        val refinesPaths = sharedCompilationData.refinesPaths.files
        if (refinesPaths.isNotEmpty()) {
            add("-Xrefines-paths=${refinesPaths.joinToString(separator = ",") { it.absolutePath }}")
        }
    }

    if (compilerOptions.usesK2.get()) {
        /*
        For now, we only pass multiplatform structure to K2 for platform compilations
        Metadata compilations will compile against pre-compiled klibs from their dependsOn
        */
        if (sharedCompilationData == null) {
            add("-Xfragments=${k2MultiplatformCompilationData.fragmentsCompilerArgs.joinToString(",")}")
            add("-Xfragment-sources=${k2MultiplatformCompilationData.fragmentSourcesCompilerArgs.joinToString(",")}")
            add("-Xfragment-refines=${k2MultiplatformCompilationData.fragmentRefinesCompilerArgs.joinToString(",")}")
        }
    }

    addAll(buildKotlinNativeCompileCommonArgs(languageSettings, compilerOptions, compilerPlugins))

    addAll(source.map { it.absolutePath })

    if (!compilerOptions.usesK2.get() && !commonSourcesTree.isEmpty) {
        add("-Xcommon-sources=${commonSourcesTree.joinToString(separator = ",") { it.absolutePath }}")
    }
}

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
    additionalOptions: Collection<String>
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
    libraries: List<File>
): List<String> = mutableListOf<String>().apply {
    addKey("-opt", optimized)
    addKey("-g", debuggable)
    addKey("-ea", debuggable)
    addArg("-target", target.name)
    addArg("-p", outputKind.name.toLowerCaseAsciiOnly())
    addArg("-o", outFile.absolutePath)
    libraries.forEach { addArg("-l", it.absolutePath) }
}

internal fun buildKotlinNativeCompileCommonArgs(
    languageSettings: LanguageSettings,
    compilerOptions: KotlinCommonCompilerOptions,
    compilerPlugins: List<CompilerPluginData>
): List<String> = mutableListOf<String>().apply {
    add("-Xmulti-platform")
    addKey("-no-endorsed-libs", true)

    compilerPlugins.forEach { plugin ->
        plugin.files.map { it.canonicalPath }.sorted().forEach { add("-Xplugin=$it") }
        addArgs("-P", plugin.options.arguments)
    }

    languageSettings.run {
        enabledLanguageFeatures.forEach { add("-XXLanguage:+$it") }
    }

    addArgIfNotNull("-language-version", compilerOptions.languageVersion.orNull?.version)
    addArgIfNotNull("-api-version", compilerOptions.apiVersion.orNull?.version)
    addKey("-Werror", compilerOptions.allWarningsAsErrors.get())
    addKey("-nowarn", compilerOptions.suppressWarnings.get())
    addKey("-verbose", compilerOptions.verbose.get())
    addKey("-progressive", compilerOptions.progressiveMode.get())
    if (compilerOptions.useK2.get()) {
        add("-Xuse-k2")
    }
    compilerOptions.optIn.get().forEach { add("-opt-in=$it") }

    addAll(compilerOptions.freeCompilerArgs.get())
}

internal fun buildKotlinNativeCommonArgs(
    toolOptions: KotlinCommonCompilerToolOptions,
    compilerPlugins: List<CompilerPluginData>
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
