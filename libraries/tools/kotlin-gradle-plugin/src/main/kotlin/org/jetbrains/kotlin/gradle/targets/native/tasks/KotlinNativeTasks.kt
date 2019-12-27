/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.KonanCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KonanInteropRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

// TODO: It's just temporary tasks used while KN isn't integrated with Big Kotlin compilation infrastructure.
// region Useful extensions
internal fun MutableList<String>.addArg(parameter: String, value: String) {
    add(parameter)
    add(value)
}

internal fun MutableList<String>.addArgs(parameter: String, values: Iterable<String>) {
    values.forEach {
        addArg(parameter, it)
    }
}

internal fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
    if (value != null) {
        addArg(parameter, value)
    }
}

internal fun MutableList<String>.addKey(key: String, enabled: Boolean) {
    if (enabled) {
        add(key)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
    values.files.forEach {
        addArg(parameter, it.canonicalPath)
    }
}

internal fun MutableList<String>.addFileArgs(parameter: String, values: Collection<FileCollection>) {
    values.forEach {
        addFileArgs(parameter, it)
    }
}

private fun File.providedByCompiler(project: Project): Boolean =
    toPath().startsWith(project.file(project.konanHome).resolve("klib").toPath())

// We need to filter out interop duplicates because we create copy of them for IDE.
// TODO: Remove this after interop rework.
private fun FileCollection.filterOutPublishableInteropLibs(project: Project): FileCollection {
    val libDirectories = project.rootProject.allprojects.map { it.buildDir.resolve("libs").absoluteFile.toPath() }
    return filter { file ->
        !(file.name.contains("-cinterop-") && libDirectories.any { file.toPath().startsWith(it) })
    }
}

private fun Collection<File>.filterExternalKlibs(project: Project) = filter {
    // Support only klib files for now.
    it.extension == "klib" && !it.providedByCompiler(project)
}

// endregion
abstract class AbstractKotlinNativeCompile<T : KotlinCommonToolOptions> : AbstractCompile() {

    init {
        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }

    @get:Internal
    abstract val compilation: KotlinNativeCompilation

    // region inputs/outputs
    @get:Input
    abstract val outputKind: CompilerOutputKind

    @get:Input
    abstract val optimized: Boolean

    @get:Input
    abstract val debuggable: Boolean

    @get:Internal
    abstract val baseName: String

    // Inputs and outputs
    val libraries: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)

    override fun getClasspath(): FileCollection = libraries
    override fun setClasspath(configuration: FileCollection?) {
        throw UnsupportedOperationException("Setting classpath directly is unsupported.")
    }

    val target: String
        @Input get() = compilation.konanTarget.name

    // region Compiler options.
    @get:Internal
    abstract val kotlinOptions: T
    abstract fun kotlinOptions(fn: T.() -> Unit)
    abstract fun kotlinOptions(fn: Closure<*>)

    @get:Input
    abstract val additionalCompilerOptions: Collection<String>

    @get:Internal
    val languageSettings: LanguageSettingsBuilder
        get() = compilation.defaultSourceSet.languageSettings

    @get:Input
    val progressiveMode: Boolean
        get() = languageSettings.progressiveMode
    // endregion.

    @get:Input
    val enableEndorsedLibs: Boolean
        get() = compilation.enableEndorsedLibs

    val kotlinNativeVersion: String
        @Input get() = project.konanVersion.toString()

    // OutputFile is located under the destinationDir, so there is no need to register it as a separate output.
    @Internal
    val outputFile: Provider<File> = project.provider {
        val konanTarget = compilation.konanTarget

        val prefix = outputKind.prefix(konanTarget)
        val suffix = outputKind.suffix(konanTarget)
        val filename = "$prefix$baseName$suffix".let {
            when {
                outputKind == FRAMEWORK ->
                    it.asValidFrameworkName()
                outputKind in listOf(STATIC, DYNAMIC) || outputKind == PROGRAM && konanTarget == KonanTarget.WASM32 ->
                    it.replace('-', '_')
                else -> it
            }
        }

        destinationDir.resolve(filename)
    }

    // endregion
    @Internal
    val compilerPluginOptions = CompilerPluginOptions()

    val compilerPluginCommandLine
        @Input get() = compilerPluginOptions.arguments

    @Optional
    @InputFiles
    var compilerPluginClasspath: FileCollection? = null

    // Used by IDE via reflection.
    val serializedCompilerArguments: List<String>
        @Internal get() = buildCommonArgs()

    // Used by IDE via reflection.
    val defaultSerializedCompilerArguments: List<String>
        @Internal get() = buildCommonArgs(true)

    // Args used by both the compiler and IDEA.
    protected open fun buildCommonArgs(defaultsOnly: Boolean = false): List<String> = mutableListOf<String>().apply {
        add("-Xmulti-platform")

        if (!enableEndorsedLibs) {
            add("-no-endorsed-libs")
        }

        // Compiler plugins.
        compilerPluginClasspath?.let { pluginClasspath ->
            pluginClasspath.map { it.canonicalPath }.sorted().forEach { path ->
                add("-Xplugin=$path")
            }
            compilerPluginOptions.arguments.forEach {
                add("-P")
                add(it)
            }
        }

        // kotlin options
        addKey("-Werror", kotlinOptions.allWarningsAsErrors)
        addKey("-nowarn", kotlinOptions.suppressWarnings)
        addKey("-verbose", kotlinOptions.verbose)
        addKey("-progressive", progressiveMode)

        if (!defaultsOnly) {
            addAll(additionalCompilerOptions)
        }
    }

    // Args passed to the compiler only (except sources).
    protected open fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        addKey("-opt", optimized)
        addKey("-g", debuggable)
        addKey("-ea", debuggable)

        addArg("-target", target)
        addArg("-p", outputKind.name.toLowerCase())

        addArg("-o", outputFile.get().absolutePath)

        // Libraries.
        libraries.files.filterExternalKlibs(project).forEach { library ->
            addArg("-l", library.absolutePath)
        }
    }

    // Sources passed to the compiler.
    // We add sources after all other arguments to make the command line more readable and simplify debugging.
    protected abstract fun buildSourceArgs(): List<String>

    private fun buildArgs(): List<String> =
        buildCompilerArgs() + buildCommonArgs() + buildSourceArgs()

    @TaskAction
    open fun compile() {
        val output = outputFile.get()
        output.parentFile.mkdirs()
        KonanCompilerRunner(project).run(buildArgs())
    }
}

/**
 * A task producing a klibrary from a compilation.
 */
open class KotlinNativeCompile : AbstractKotlinNativeCompile<KotlinCommonOptions>(), KotlinCompile<KotlinCommonOptions> {
    @Internal
    override lateinit var compilation: KotlinNativeCompilation

    @get:Input
    override val outputKind = LIBRARY

    @get:Input
    override val optimized = false

    @get:Input
    override val debuggable = true

    @get:Internal
    override val baseName: String
        get() = if (compilation.isMainCompilation) project.name else compilation.name

    // Inputs and outputs.
    // region Sources.
    @InputFiles
    @SkipWhenEmpty
    override fun getSource(): FileTree = project.files(compilation.allSources).asFileTree

    private val commonSources: FileCollection
        // Already taken into account in getSources method.
        get() = project.files(compilation.commonSources).asFileTree

    private val friendModule: FileCollection?
        get() = project.files(
            project.provider { compilation.friendCompilations.map { it.output.allOutputs } + compilation.friendArtifacts }
        )
    // endregion.

    // region Language settings imported from a SourceSet.
    val languageVersion: String?
        @Optional @Input get() = languageSettings.languageVersion

    val apiVersion: String?
        @Optional @Input get() = languageSettings.apiVersion

    val enabledLanguageFeatures: Set<String>
        @Input get() = languageSettings.enabledLanguageFeatures

    val experimentalAnnotationsInUse: Set<String>
        @Input get() = languageSettings.experimentalAnnotationsInUse
    // endregion.

    // region Kotlin options.
    private inner class NativeCompileOptions : KotlinCommonOptions {
        override var apiVersion: String?
            get() = languageSettings.apiVersion
            set(value) { languageSettings.apiVersion = value }

        override var languageVersion: String?
            get() = this@KotlinNativeCompile.languageVersion
            set(value) { languageSettings.languageVersion = value }

        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false

        // TODO: Drop extraOpts in 1.3.70 and create a list here directly
        // Delegate for compilations's extra options.
        override var freeCompilerArgs: List<String>
            get() = compilation.extraOptsNoWarn
            set(value) {
                compilation.extraOptsNoWarn = value.toMutableList()
            }
    }

    @get:Input
    override val additionalCompilerOptions: Collection<String>
        get() = kotlinOptions.freeCompilerArgs

    override val kotlinOptions: KotlinCommonOptions = NativeCompileOptions()

    override fun kotlinOptions(fn: KotlinCommonOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    override fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }
    // endregion.

    // region Building args.
    override fun buildCommonArgs(defaultsOnly: Boolean): List<String> = mutableListOf<String>().apply {
        addAll(super.buildCommonArgs(defaultsOnly))

        // Language features.
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)
        enabledLanguageFeatures.forEach { featureName ->
            add("-XXLanguage:+$featureName")
        }
        experimentalAnnotationsInUse.forEach { annotationName ->
            add("-Xopt-in=$annotationName")
        }
    }

    override fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        addAll(super.buildCompilerArgs())

        val friends = friendModule?.files
        if (friends != null && friends.isNotEmpty()) {
            addArg("-friend-modules", friends.map { it.absolutePath }.joinToString(File.pathSeparator))
        }
    }

    override fun buildSourceArgs(): List<String> = mutableListOf<String>().apply {
        addAll(getSource().map { it.absolutePath })
        if (!commonSources.isEmpty) {
            add("-Xcommon-sources=${commonSources.map { it.absolutePath }.joinToString(separator = ",")}")
        }
    }
    // endregion.
}

/**
 * A task producing a final binary from a compilation.
 */
open class KotlinNativeLink : AbstractKotlinNativeCompile<KotlinCommonToolOptions>() {

    init {
        if (!linkFromSources) {
            dependsOn(project.provider { compilation.compileKotlinTask })
        }
    }

    @Internal
    lateinit var binary: NativeBinary

    @get:Internal
    override val compilation: KotlinNativeCompilation
        get() = binary.compilation

    @Internal // Taken into account by getSources().
    val intermediateLibrary: Provider<File> = project.provider {
        compilation.compileKotlinTask.outputFile.get()
    }

    @InputFiles
    @SkipWhenEmpty
    override fun getSource(): FileTree =
        if (linkFromSources) {
            // Allow a user to force the old behaviour of a link task.
            // TODO: Remove in 1.3.70.
            project.files(compilation.allSources).asFileTree
        } else {
            project.files(intermediateLibrary.get()).asFileTree
        }

    @get:Input
    override val outputKind: CompilerOutputKind
        get() = binary.outputKind.compilerOutputKind

    @get:Input
    override val optimized: Boolean
        get() = binary.optimized

    @get:Input
    override val debuggable: Boolean
        get() = binary.debuggable

    @get:Internal
    override val baseName: String
        get() = binary.baseName

    @get:Input
    protected val konanCacheKind: NativeCacheKind
        get() = project.konanCacheKind

    inner class NativeLinkOptions: KotlinCommonToolOptions {
        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false
        override var freeCompilerArgs: List<String> = listOf()
    }

    // We propagate compilation free args to the link task for now (see KT-33717).
    @get:Input
    override val additionalCompilerOptions: Collection<String>
        get() = kotlinOptions.freeCompilerArgs + compilation.kotlinOptions.freeCompilerArgs

    override val kotlinOptions: KotlinCommonToolOptions = NativeLinkOptions()

    override fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    override fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }

    //region language settings inputs for the [linkFromSources] mode.
    // TODO: Remove in 1.3.70.
    @get:Optional
    @get:Input
    val languageVersion: String?
        get() = languageSettings.languageVersion.takeIf { linkFromSources }

    @get:Optional
    @get:Input
    val apiVersion: String?
        get() = languageSettings.apiVersion.takeIf { linkFromSources }

    @get:Optional
    @get:Input
    val enabledLanguageFeatures: Set<String>?
        get() = languageSettings.enabledLanguageFeatures.takeIf { linkFromSources }

    @get:Optional
    @get:Input
    val experimentalAnnotationsInUse: Set<String>?
        get() = languageSettings.experimentalAnnotationsInUse.takeIf { linkFromSources }
    // endregion.

    // Binary-specific options.
    @get:Optional
    @get:Input
    val entryPoint: String?
        get() = (binary as? Executable)?.entryPoint

    @get:Input
    val linkerOpts: List<String>
        get() = binary.linkerOpts

    @get:Input
    val processTests: Boolean
        get() = binary is TestExecutable

    @get:InputFiles
    val exportLibraries: FileCollection
        get() = binary.let {
            if (it is Framework) {
                project.configurations.getByName(it.exportConfigurationName)
            } else {
                project.files()
            }
        }

    @get:Input
    val isStaticFramework: Boolean
        get() = binary.let { it is Framework && it.isStatic }

    @get:Input
    val embedBitcode: Framework.BitcodeEmbeddingMode
        get() = (binary as? Framework)?.embedBitcode ?: Framework.BitcodeEmbeddingMode.DISABLE

    // This property allows a user to force the old behaviour of a link task
    // to workaround issues that may occur after switching to the two-stage linking.
    // If it is specified, the final binary is built directly from sources instead of a klib.
    // TODO: Remove it in 1.3.70.
    private val linkFromSources: Boolean
        get() = project.hasProperty(LINK_FROM_SOURCES_PROPERTY)

    override fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        addAll(super.buildCompilerArgs())

        addAll(CacheBuilder(project, binary).buildCompilerArgs())

        addKey("-tr", processTests)
        addArgIfNotNull("-entry", entryPoint)
        when (embedBitcode) {
            Framework.BitcodeEmbeddingMode.MARKER -> add("-Xembed-bitcode-marker")
            Framework.BitcodeEmbeddingMode.BITCODE -> add("-Xembed-bitcode")
            else -> { /* Do nothing. */ }
        }
        linkerOpts.forEach {
            addArg("-linker-option", it)
        }
        exportLibraries.files.filterExternalKlibs(project).forEach {
            add("-Xexport-library=${it.absolutePath}")
        }
        addKey("-Xstatic-framework", isStaticFramework)

        // Allow a user to force the old behaviour of a link task.
        // TODO: Remove in 1.3.70.
        if (!linkFromSources) {
            languageSettings.let {
                addArgIfNotNull("-language-version", it.languageVersion)
                addArgIfNotNull("-api-version", it.apiVersion)
                it.enabledLanguageFeatures.forEach { featureName ->
                    add("-XXLanguage:+$featureName")
                }
                it.experimentalAnnotationsInUse.forEach { annotationName ->
                    add("-Xopt-in=$annotationName")
                }
            }
        }
    }

    override fun buildSourceArgs(): List<String> {
        return if (!linkFromSources) {
            listOf("-Xinclude=${intermediateLibrary.get().absolutePath}")
        } else {
            // Allow a user to force the old behaviour of a link task.
            // TODO: Remove in 1.3.70.
            mutableListOf<String>().apply {
                val friendCompilations = compilation.friendCompilations
                val friendFiles = if (friendCompilations.isNotEmpty())
                    project.files(
                        project.provider { friendCompilations.map { it.output.allOutputs } + compilation.friendArtifacts }
                    )
                else null

                if (friendFiles != null && !friendFiles.isEmpty) {
                    addArg("-friend-modules", friendFiles.joinToString(File.pathSeparator) { it.absolutePath })
                }

                addAll(project.files(compilation.allSources).map { it.absolutePath })
                if (!compilation.commonSources.isEmpty) {
                    add("-Xcommon-sources=${compilation.commonSources.joinToString(separator = ",") { it.absolutePath }}")
                }
            }
        }
    }

    private fun validatedExportedLibraries() {
        val exportConfiguration = exportLibraries as? Configuration ?: return
        val apiFiles = project.configurations.getByName(compilation.apiConfigurationName).files.filterExternalKlibs(project)

        val failed = mutableSetOf<Dependency>()
        exportConfiguration.allDependencies.forEach {
            val dependencyFiles = exportConfiguration.files(it).filterExternalKlibs(project)
            if (!apiFiles.containsAll(dependencyFiles)) {
                failed.add(it)
            }
        }

        check(failed.isEmpty()) {
            val failedDependenciesList = failed.joinToString(separator = "\n") {
                when (it) {
                    is FileCollectionDependency -> "|Files: ${it.files.files}"
                    is ProjectDependency -> "|Project ${it.dependencyProject.path}"
                    else -> "|${it.group}:${it.name}:${it.version}"
                }
            }

            """
                |Following dependencies exported in the ${binary.name} binary are not specified as API-dependencies of a corresponding source set:
                |
                $failedDependenciesList
                |
                |Please add them in the API-dependencies and rerun the build.
            """.trimMargin()
        }
    }

    @TaskAction
    override fun compile() {
        validatedExportedLibraries()
        super.compile()
    }

    companion object {
        private const val LINK_FROM_SOURCES_PROPERTY = "kotlin.native.linkFromSources"
    }
}

class CacheBuilder(val project: Project, val binary: NativeBinary) {
    private val compilation: KotlinNativeCompilation
        get() = binary.compilation

    private val optimized: Boolean
        get() = binary.optimized

    private val debuggable: Boolean
        get() = binary.debuggable

    private val konanCacheKind: NativeCacheKind
        get() = project.konanCacheKind

    // Inputs and outputs
    private val libraries: FileCollection
        get() = compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)

    private val target: String
        get() = compilation.konanTarget.name

    private val optionsAwareCacheName get() = "$target${if (debuggable) "-g" else ""}$konanCacheKind"

    private val rootCacheDirectory
        get() = File(project.konanHome).resolve("klib/cache/$optionsAwareCacheName")

    private fun getAllDependencies(dependency: ResolvedDependency): Set<ResolvedDependency> {
        val allDependencies = mutableSetOf<ResolvedDependency>()

        fun traverseAllDependencies(dependency: ResolvedDependency) {
            if (dependency in allDependencies)
                return
            allDependencies.add(dependency)
            dependency.children.forEach { traverseAllDependencies(it) }
        }

        dependency.children.forEach { traverseAllDependencies(it) }
        return allDependencies
    }

    private fun ByteArray.toHexString() = joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

    private fun computeDependenciesHash(dependency: ResolvedDependency): String {
        val allArtifactsPaths =
            (dependency.moduleArtifacts + getAllDependencies(dependency).flatMap { it.moduleArtifacts })
                .map { it.file.absolutePath }
                .distinct()
                .sortedBy { it }
                .joinToString("|") { it }
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(allArtifactsPaths.toByteArray(StandardCharsets.UTF_8))
        return hash.toHexString()
    }

    private fun getCacheDirectory(dependency: ResolvedDependency): File {
        val moduleCacheDirectory = File(rootCacheDirectory, dependency.moduleName)
        val versionCacheDirectory = File(moduleCacheDirectory, dependency.moduleVersion)
        return File(versionCacheDirectory, computeDependenciesHash(dependency))
    }

    private fun needCache(libraryPath: String) =
        libraryPath.startsWith(project.gradle.gradleUserHomeDir.absolutePath) && libraryPath.endsWith(".klib")

    private fun ensureDependencyPrecached(dependency: ResolvedDependency, visitedDependencies: MutableSet<ResolvedDependency>) {
        if (dependency in visitedDependencies)
            return
        visitedDependencies += dependency
        dependency.children.forEach { ensureDependencyPrecached(it, visitedDependencies) }

        val artifactsToAddToCache = dependency.moduleArtifacts.filter { needCache(it.file.absolutePath) }
        if (artifactsToAddToCache.isEmpty()) return

        val dependenciesCacheDirectories = getAllDependencies(dependency)
            .map { childDependency ->
                val hasKlibs = childDependency.moduleArtifacts.any { it.file.absolutePath.endsWith(".klib") }
                val cacheDirectory = getCacheDirectory(childDependency)
                // We can only compile klib to cache if all of its dependencies are also cached.
                if (hasKlibs && !cacheDirectory.exists())
                    return
                cacheDirectory
            }
            .filter { it.exists() }
        val cacheDirectory = getCacheDirectory(dependency)
        cacheDirectory.mkdirs()

        val artifactsLibraries = artifactsToAddToCache
            .map { createKotlinLibrary(org.jetbrains.kotlin.konan.file.File(it.file.absolutePath)) }
            .associateBy { it.uniqueName }

        // Top sort artifacts.
        val sortedLibraries = mutableListOf<KotlinLibrary>()
        val visitedLibraries = mutableSetOf<KotlinLibrary>()

        fun dfs(library: KotlinLibrary) {
            visitedLibraries += library
            library.unresolvedDependencies
                .map { artifactsLibraries[it.path] }
                .forEach {
                    if (it != null && it !in visitedLibraries)
                        dfs(it)
                }
            sortedLibraries += library
        }

        for (library in artifactsLibraries.values)
            if (library !in visitedLibraries)
                dfs(library)

        for (library in sortedLibraries) {
            if (File(cacheDirectory, library.uniqueName.cachedName).exists())
                continue
            project.logger.info("Compiling ${library.uniqueName} to cache")
            val args = mutableListOf(
                "-p", konanCacheKind.produce!!,
                "-target", target
            )
            if (debuggable)
                args += "-g"
            args += "-Xadd-cache=${library.libraryFile.absolutePath}"
            args += "-Xcache-directory=${cacheDirectory.absolutePath}"
            args += "-Xcache-directory=${rootCacheDirectory.absolutePath}"

            dependenciesCacheDirectories.forEach {
                args += "-Xcache-directory=${it.absolutePath}"
            }
            getAllDependencies(dependency)
                .flatMap { it.moduleArtifacts }
                .map { it.file }
                .filterExternalKlibs(project)
                .forEach {
                    args += "-l"
                    args += it.absolutePath
                }
            library.unresolvedDependencies
                .mapNotNull { artifactsLibraries[it.path] }
                .forEach {
                    args += "-l"
                    args += it.libraryFile.absolutePath
                }
            KonanCompilerRunner(project).run(args)
        }
    }

    private val String.cachedName
        get() = konanCacheKind.outputKind?.let {
            "${it.prefix(compilation.konanTarget)}${this}-cache${it.suffix(compilation.konanTarget)}"
        } ?: error("No output for kind $konanCacheKind")

    private fun ensureCompilerProvidedLibPrecached(platformLibName: String, platformLibs: Map<String, File>, visitedLibs: MutableSet<String>) {
        if (platformLibName in visitedLibs)
            return
        visitedLibs += platformLibName
        val platformLib = platformLibs[platformLibName] ?: error("$platformLibName is not found in platform libs")
        if (File(rootCacheDirectory, platformLibName.cachedName).exists())
            return
        for (dependency in createKotlinLibrary(org.jetbrains.kotlin.konan.file.File(platformLib.absolutePath)).unresolvedDependencies)
            ensureCompilerProvidedLibPrecached(dependency.path, platformLibs, visitedLibs)
        project.logger.info("Compiling $platformLibName (${visitedLibs.size}/${platformLibs.size}) to cache")
        val args = mutableListOf(
            "-p", konanCacheKind.produce!!,
            "-target", target
        )
        if (debuggable)
            args += "-g"
        args += "-Xadd-cache=${platformLib.absolutePath}"
        args += "-Xcache-directory=${rootCacheDirectory.absolutePath}"
        KonanCompilerRunner(project).run(args)
    }

    private fun ensureCompilerProvidedLibsPrecached() {
        val platformLibs = libraries.filter { it.providedByCompiler(project) }.associateBy { it.name }
        val visitedLibs = mutableSetOf<String>()
        for (platformLibName in platformLibs.keys)
            ensureCompilerProvidedLibPrecached(platformLibName, platformLibs, visitedLibs)
    }

    private val KonanTarget.cacheWorks
        get() = this == KonanTarget.IOS_X64 || this == KonanTarget.MACOS_X64

    fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        if (konanCacheKind != NativeCacheKind.NONE && !optimized && compilation.konanTarget.cacheWorks) {
            rootCacheDirectory.mkdirs()
            ensureCompilerProvidedLibsPrecached()
            add("-Xcache-directory=${rootCacheDirectory.absolutePath}")
            val visitedDependencies = mutableSetOf<ResolvedDependency>()
            val allCacheDirectories = mutableSetOf<String>()
            val compileDependencyConfiguration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
            for (root in compileDependencyConfiguration.resolvedConfiguration.firstLevelModuleDependencies) {
                ensureDependencyPrecached(root, visitedDependencies)
                for (dependency in listOf(root) + getAllDependencies(root)) {
                    val cacheDirectory = getCacheDirectory(dependency)
                    if (cacheDirectory.exists())
                        allCacheDirectories += cacheDirectory.absolutePath
                }
            }
            for (cacheDirectory in allCacheDirectories)
                add("-Xcache-directory=$cacheDirectory")
        }
    }
}

open class CInteropProcess : DefaultTask() {

    @Internal
    lateinit var settings: DefaultCInteropSettings

    @Internal // Taken into account in the outputFileProvider property
    lateinit var destinationDir: Provider<File>

    val konanTarget: KonanTarget
        @Internal get() = settings.compilation.konanTarget

    val interopName: String
        @Internal get() = settings.name

    val outputFileName: String
        @Internal get() = with(CompilerOutputKind.LIBRARY) {
            val baseName = settings.compilation.let {
                if (it.isMainCompilation) project.name else it.name
            }
            val suffix = suffix(konanTarget)
            return "$baseName-cinterop-$interopName$suffix"
        }

    @get:Internal
    val outputFile: File
        get() = outputFileProvider.get()

    // Inputs and outputs.

    @OutputFile
    val outputFileProvider: Provider<File> =
        project.provider { destinationDir.get().resolve(outputFileName) }

    val defFile: File
        @InputFile get() = settings.defFile

    val packageName: String?
        @Optional @Input get() = settings.packageName

    val compilerOpts: List<String>
        @Input get() = settings.compilerOpts

    val linkerOpts: List<String>
        @Input get() = settings.linkerOpts

    val headers: FileCollection
        @InputFiles get() = settings.headers

    val allHeadersDirs: Set<File>
        @Input get() = settings.includeDirs.allHeadersDirs.files

    val headerFilterDirs: Set<File>
        @Input get() = settings.includeDirs.headerFilterDirs.files

    val libraries: FileCollection
        @InputFiles get() = settings.dependencyFiles.filterOutPublishableInteropLibs(project)

    val extraOpts: List<String>
        @Input get() = settings.extraOpts

    val kotlinNativeVersion: String
        @Input get() = project.konanVersion.toString()

    // Task action.
    @TaskAction
    fun processInterop() {
        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.absolutePath)

            addArgIfNotNull("-target", konanTarget.visibleName)
            addArgIfNotNull("-def", defFile.canonicalPath)
            addArgIfNotNull("-pkg", packageName)

            addFileArgs("-header", headers)

            compilerOpts.forEach {
                addArg("-compiler-option", it)
            }

            linkerOpts.forEach {
                addArg("-linker-option", it)
            }

            libraries.files.filterExternalKlibs(project).forEach { library ->
                addArg("-library", library.absolutePath)
            }

            addArgs("-compiler-option", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KonanInteropRunner(project).run(args)
    }
}
