/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.native.internal.isAllowCommonizer
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.properties.saveToFile
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.util.Logger as KLogger

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

/**
 * We pass to the compiler:
 *
 *    - Only *.klib files and directories (normally containing an unpacked klib).
 *      A dependency configuration may contain jar files
 *      (e.g. when a common artifact was directly added to commonMain source set).
 *      So, we need to filter out such artifacts.
 *
 *    - Only existing files. We don't compile a klib if there are no sources
 *      for it (NO-SOURCE check). So we need to take this case into account
 *      and skip libraries that were not compiled. See also: GH-2617 (K/N repo).
 */
private fun Collection<File>.filterKlibsPassedToCompiler(project: Project) = filter {
    (it.extension == "klib" || it.isDirectory) && it.exists()
}

// endregion
abstract class AbstractKotlinNativeCompile<T : KotlinCommonToolOptions, K : AbstractKotlinNativeCompilation> : AbstractCompile() {
    @get:Internal
    abstract val compilation: Provider<K>

    @get:Internal
    internal val compilationIsShared by lazy {
        compilation.get() is KotlinSharedNativeCompilation
    }

    // region inputs/outputs
    @get:Input
    abstract val outputKind: CompilerOutputKind

    @get:Input
    abstract val optimized: Boolean

    @get:Input
    abstract val debuggable: Boolean

    @get:Internal
    abstract val baseName: String

    @get:Internal
    internal val objects = project.objects

    @get:Internal
    internal val konanTarget by lazy {
        compilation.get().konanTarget
    }

    // Inputs and outputs
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.getSource()
    }

    @get:Classpath
    val libraries: FileCollection by project.provider {
        // Avoid resolving these dependencies during task graph construction when we can't build the target:
        if (konanTarget.enabledOnCurrentHost)
            compilation.get().compileDependencyFiles.filterOutPublishableInteropLibs(project)
        else objects.fileCollection()
    }

    @Deprecated("For native tasks use 'libraries' instead", ReplaceWith("libraries"))
    override fun getClasspath(): FileCollection = libraries
    override fun setClasspath(configuration: FileCollection?) {
        throw UnsupportedOperationException("Setting classpath directly is unsupported.")
    }

    @get:Input
    val target: String by project.provider { compilation.get().konanTarget.name }

    // region Compiler options.
    @get:Internal
    abstract val kotlinOptions: T
    abstract fun kotlinOptions(fn: T.() -> Unit)
    abstract fun kotlinOptions(fn: Closure<*>)

    @get:Input
    abstract val additionalCompilerOptions: Provider<Collection<String>>

    @get:Internal
    val languageSettings: LanguageSettingsBuilder by lazy {
        compilation.get().defaultSourceSet.languageSettings
    }

    @get:Input
    val progressiveMode: Boolean
        get() = languageSettings.progressiveMode
    // endregion.

    @get:Input
    val enableEndorsedLibs: Boolean by project.provider { compilation.map { it.enableEndorsedLibs }.get() }

    @get:Input
    val kotlinNativeVersion: String
        get() = project.konanVersion.toString()

    @Internal
    open val outputFile: Provider<File> = project.provider {
        val prefix = outputKind.prefix(konanTarget)
        val suffix = outputKind.suffix(konanTarget)
        val filename = "$prefix${baseName}$suffix".let {
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

    @get:Input
    val compilerPluginCommandLine
        get() = compilerPluginOptions.arguments

    @Optional
    @Classpath
    var compilerPluginClasspath: FileCollection? = null

    // Used by IDE via reflection.
    @get:Internal
    val serializedCompilerArguments: List<String>
        get() = buildCommonArgs()

    // Used by IDE via reflection.
    @get:Internal
    val defaultSerializedCompilerArguments: List<String>
        get() = buildCommonArgs(true)

    @get:Internal
    internal val languageSettingsBuilder by lazy {
        compilation.get().defaultSourceSet.languageSettings
    }

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
            addAll(additionalCompilerOptions.get())
        }

        (languageSettingsBuilder as? DefaultLanguageSettingsBuilder)?.run {
            addAll(freeCompilerArgs)
        }
    }

    @get:Input
    @get:Optional
    internal val konanTargetsForManifest: String by project.provider {
        (compilation as? KotlinSharedNativeCompilation)
            ?.konanTargets
            ?.joinToString(separator = " ") { it.visibleName }
            .orEmpty()
    }

    @get:Internal
    internal val manifestFile: Provider<File>
        get() = project.provider {
            val inputManifestFile = project.buildDir.resolve("tmp/$name/inputManifest")
            inputManifestFile
        }

    // Args passed to the compiler only (except sources).
    protected open fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        addKey("-opt", optimized)
        addKey("-g", debuggable)
        addKey("-ea", debuggable)

        addArg("-target", konanTarget.name)
        addArg("-p", outputKind.name.toLowerCase())

        if (compilationIsShared) {
            add("-Xexpect-actual-linker")
            add("-Xmetadata-klib")
            addArg("-manifest", manifestFile.get().absolutePath)
            if (project.isAllowCommonizer()) add("-no-default-libs")
        }

        addArg("-o", outputFile.get().absolutePath)

        // Libraries.
        libraries.files.filterKlibsPassedToCompiler(project).forEach { library ->
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

        if (compilationIsShared) {
            val manifestFile: File = manifestFile.get()
            manifestFile.ensureParentDirsCreated()
            val properties = java.util.Properties()
            properties[KLIB_PROPERTY_NATIVE_TARGETS] = konanTargetsForManifest
            properties.saveToFile(org.jetbrains.kotlin.konan.file.File(manifestFile.toPath()))
        }

        KotlinNativeCompilerRunner(project).run(buildArgs())
    }
}

/**
 * A task producing a klibrary from a compilation.
 */
@CacheableTask
open class KotlinNativeCompile : AbstractKotlinNativeCompile<KotlinCommonOptions, AbstractKotlinNativeCompilation>(),
    KotlinCompile<KotlinCommonOptions> {
    @Internal
    @Transient // can't be serialized for Gradle configuration avoidance
    final override val compilation: Property<AbstractKotlinNativeCompilation> =
        project.newProperty()

    @get:Input
    override val outputKind = LIBRARY

    @get:Input
    override val optimized = false

    @get:Input
    override val debuggable = true

    @get:Internal
    override val baseName: String by
    compilation.map { if (it.isMain()) project.name else "${project.name}_${it.name}" }

    // Store as an explicit provider in order to allow Gradle Instant Execution to capture the state
//    private val allSourceProvider = compilation.map { project.files(it.allSources).asFileTree }

    @get:Input
    val moduleName: String by project.provider {
        project.klibModuleName(baseName)
    }

    @get:OutputFile
    override val outputFile: Provider<File>
        get() = super.outputFile

    @get:Input
    val shortModuleName: String by project.provider { baseName }

    // Inputs and outputs.
    // region Sources.

    @get:Internal // these sources are normally a subset of `source` ones which are already tracked
    val commonSources: ConfigurableFileCollection = project.files()

//    private val commonSources: FileCollection by lazy {
//        // Already taken into account in getSources method.
//        project.files(compilation.map { it.commonSources }).asFileTree
//    }

    private val commonSourcesTree: FileTree
        get() = commonSources.asFileTree


    private val friendModule: FileCollection by compilation.map { compilationInstance ->
        project.files(
            compilationInstance.associateWithTransitiveClosure.map { it.output.allOutputs },
            compilationInstance.friendArtifacts
        )
    }
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
    override val kotlinOptions: KotlinCommonOptions by lazy {
        compilation.get().kotlinOptions
    }

    @get:Input
    override val additionalCompilerOptions: Provider<Collection<String>> = project.provider {
        kotlinOptions.freeCompilerArgs
    }

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

        // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
        addArg("-module-name", moduleName)
        add("-Xshort-module-name=$shortModuleName")
        val friends = friendModule.files
        if (friends != null && friends.isNotEmpty()) {
            addArg("-friend-modules", friends.map { it.absolutePath }.joinToString(File.pathSeparator))
        }
    }

    override fun buildSourceArgs(): List<String> = mutableListOf<String>().apply {
        addAll(getSource().map { it.absolutePath })
        if (!commonSourcesTree.isEmpty) {
            add("-Xcommon-sources=${commonSourcesTree.map { it.absolutePath }.joinToString(separator = ",")}")
        }
    }
    // endregion.
}

/**
 * A task producing a final binary from a compilation.
 */
@CacheableTask
open class KotlinNativeLink : AbstractKotlinNativeCompile<KotlinCommonToolOptions, KotlinNativeCompilation>() {

    @get:Internal
    @Transient // can't be serialized for Gradle configuration avoidance
    final override val compilation: Property<KotlinNativeCompilation> = project.newProperty() { binary.compilation }

    init {
        dependsOn(project.provider { compilation.get().compileKotlinTask })
        // Frameworks actively uses symlinks.
        // Gradle build cache transforms symlinks into regular files https://guides.gradle.org/using-build-cache/#symbolic_links
        outputs.cacheIf { outputKind != FRAMEWORK }
    }

    @Internal
    lateinit var binary: NativeBinary

    @Internal // Taken into account by getSources().
    val intermediateLibrary: Provider<File> = compilation.map { compilationInstance ->
        compilationInstance.compileKotlinTask.outputFile.get()
    }

    @InputFiles
    @SkipWhenEmpty
    override fun getSource(): FileTree =
        objects.fileCollection().from(intermediateLibrary.get()).asFileTree

    @OutputDirectory
    override fun getDestinationDir(): File {
        return binary.outputDirectory
    }

    override fun setDestinationDir(destinationDir: File) {
        binary.outputDirectory = destinationDir
    }

    override val outputKind: CompilerOutputKind
        @Input get() = binary.outputKind.compilerOutputKind

    override val optimized: Boolean
        @Input get() = binary.optimized

    override val debuggable: Boolean
        @Input get() = binary.debuggable

    override val baseName: String
        @Input get() = binary.baseName

    @get:Input
    protected val konanCacheKind: NativeCacheKind by lazy {
        project.getKonanCacheKind(konanTarget)
    }

    inner class NativeLinkOptions : KotlinCommonToolOptions {
        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false
        override var freeCompilerArgs: List<String> = listOf()
    }

    // We propagate compilation free args to the link task for now (see KT-33717).
    @get:Input
    override val additionalCompilerOptions: Provider<Collection<String>> = compilation.map { compilationInstance ->
        kotlinOptions.freeCompilerArgs + compilationInstance.kotlinOptions.freeCompilerArgs
    }

    override val kotlinOptions: KotlinCommonToolOptions = NativeLinkOptions()

    override fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    override fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }

    // Binary-specific options.
    val entryPoint: String?
        @Input
        @Optional
        get() = (binary as? Executable)?.entryPoint

    val linkerOpts: List<String>
        @Input get() = binary.linkerOpts

    val processTests: Boolean
        @Input get() = binary is TestExecutable

    @get:Classpath
    val exportLibraries: FileCollection by lazy {
        binary.let {
            if (it is AbstractNativeLibrary) {
                project.configurations.getByName(it.exportConfigurationName)
            } else {
                objects.fileCollection()
            }
        }
    }

    @get:Input
    val isStaticFramework: Boolean by project.provider {
        binary.let { it is Framework && it.isStatic }
    }

    @get:Input
    val embedBitcode: BitcodeEmbeddingMode by project.provider {
        (binary as? Framework)?.embedBitcode ?: BitcodeEmbeddingMode.DISABLE
    }

    override fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        addAll(super.buildCompilerArgs())

        addAll(CacheBuilder(project, binary, konanTarget).buildCompilerArgs())

        addKey("-tr", processTests)
        addArgIfNotNull("-entry", entryPoint)
        when (embedBitcode) {
            BitcodeEmbeddingMode.MARKER -> add("-Xembed-bitcode-marker")
            BitcodeEmbeddingMode.BITCODE -> add("-Xembed-bitcode")
            else -> { /* Do nothing. */
            }
        }
        linkerOpts.forEach {
            addArg("-linker-option", it)
        }
        exportLibraries.files.filterKlibsPassedToCompiler(project).forEach {
            add("-Xexport-library=${it.absolutePath}")
        }
        addKey("-Xstatic-framework", isStaticFramework)

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

    override fun buildSourceArgs(): List<String> =
        listOf("-Xinclude=${intermediateLibrary.get().absolutePath}")

    @get:Internal
    val apiFilesProvider =
        compilation.map { project.configurations.getByName(it.apiConfigurationName).files.filterKlibsPassedToCompiler(project) }

    private fun validatedExportedLibraries() {
        val exportConfiguration = exportLibraries as? Configuration ?: return
        val apiFiles = apiFilesProvider.get()

        val failed = mutableSetOf<Dependency>()
        exportConfiguration.allDependencies.forEach {
            val dependencyFiles = exportConfiguration.files(it).filterKlibsPassedToCompiler(project)
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
    }
}

internal class CacheBuilder(val project: Project, val binary: NativeBinary, val konanTarget: KonanTarget) {

    private val nativeSingleFileResolveStrategy: SingleFileKlibResolveStrategy
        get() = CompilerSingleFileKlibResolveAllowingIrProvidersStrategy(
            listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
        )
    private val compilation: KotlinNativeCompilation
        get() = binary.compilation

    private val optimized: Boolean
        get() = binary.optimized

    private val debuggable: Boolean
        get() = binary.debuggable

    private val konanCacheKind: NativeCacheKind
        get() = project.getKonanCacheKind(konanTarget)

    // Inputs and outputs
    private val libraries: FileCollection
        get() = compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)

    private val target: String
        get() = konanTarget.name

    private val rootCacheDirectory by lazy {
        getRootCacheDirectory(File(project.konanHome), konanTarget, debuggable, konanCacheKind)
    }

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
            .map {
                resolveSingleFileKlib(
                    KFile(it.file.absolutePath),
                    logger = GradleLoggerAdapter(project.logger),
                    strategy = nativeSingleFileResolveStrategy
                )
            }
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
            if (File(cacheDirectory, library.uniqueName.cachedName).listFilesOrEmpty().isNotEmpty())
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
                .filterKlibsPassedToCompiler(project)
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
            KotlinNativeCompilerRunner(project).run(args)
        }
    }

    private val String.cachedName
        get() = getCacheFileName(this, konanCacheKind)

    private fun ensureCompilerProvidedLibPrecached(
        platformLibName: String,
        platformLibs: Map<String, File>,
        visitedLibs: MutableSet<String>
    ) {
        if (platformLibName in visitedLibs)
            return
        visitedLibs += platformLibName
        val platformLib = platformLibs[platformLibName] ?: error("$platformLibName is not found in platform libs")
        if (File(rootCacheDirectory, platformLibName.cachedName).listFilesOrEmpty().isNotEmpty())
            return
        val unresolvedDependencies = resolveSingleFileKlib(
            KFile(platformLib.absolutePath),
            logger = GradleLoggerAdapter(project.logger),
            strategy = nativeSingleFileResolveStrategy
        ).unresolvedDependencies
        for (dependency in unresolvedDependencies)
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
        KotlinNativeCompilerRunner(project).run(args)
    }

    private fun ensureCompilerProvidedLibsPrecached() {
        val distribution = Distribution(project.konanHome)
        val platformLibs = (listOf(File(distribution.stdlib)) + File(distribution.platformLibs(konanTarget))
            .listFiles()).associateBy { it.name }
        val visitedLibs = mutableSetOf<String>()
        for (platformLibName in platformLibs.keys)
            ensureCompilerProvidedLibPrecached(platformLibName, platformLibs, visitedLibs)
    }


    fun buildCompilerArgs(): List<String> = mutableListOf<String>().apply {
        if (konanCacheKind != NativeCacheKind.NONE && !optimized && cacheWorksFor(konanTarget, project)) {
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

    private class GradleLoggerAdapter(private val gradleLogger: Logger) : KLogger {
        override fun log(message: String) = gradleLogger.info(message)
        override fun warning(message: String) = gradleLogger.warn(message)
        override fun error(message: String) = kotlin.error(message)
        override fun fatal(message: String): Nothing = kotlin.error(message)
    }

    companion object {
        internal fun getRootCacheDirectory(konanHome: File, target: KonanTarget, debuggable: Boolean, cacheKind: NativeCacheKind): File {
            require(cacheKind != NativeCacheKind.NONE) { "Usupported cache kind: ${NativeCacheKind.NONE}" }
            val optionsAwareCacheName = "$target${if (debuggable) "-g" else ""}$cacheKind"
            return konanHome.resolve("klib/cache/$optionsAwareCacheName")
        }

        internal fun getCacheFileName(baseName: String, cacheKind: NativeCacheKind): String =
            cacheKind.outputKind?.let {
                "${baseName}-cache"
            } ?: error("No output for kind $cacheKind")

        private fun getCacheableTargets(project: Project) =
            Distribution(project.konanHome)
                .properties
                .resolvablePropertyList("cacheableTargets", HostManager.hostName)
                .map { KonanTarget.predefinedTargets.getValue(it) }

        // Targets with well-tested static caches that can be enabled by default.
        // TODO: Move it to konan.properties.
        private val targetsWithStableStaticCaches =
            setOf(KonanTarget.IOS_X64, KonanTarget.MACOS_X64)

        internal fun cacheWorksFor(target: KonanTarget, project: Project) =
            target in getCacheableTargets(project)

        internal fun defaultCacheKindForTarget(target: KonanTarget): NativeCacheKind =
            if (target in targetsWithStableStaticCaches) {
                NativeCacheKind.STATIC
            } else {
                NativeCacheKind.NONE
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

    val baseKlibName: String
        @Internal get() {
            val compilationPrefix = settings.compilation.let {
                if (it.isMain()) project.name else it.name
            }
            return "$compilationPrefix-cinterop-$interopName"
        }

    val outputFileName: String
        @Internal get() = with(CompilerOutputKind.LIBRARY) {
            "$baseKlibName${suffix(konanTarget)}"
        }

    val moduleName: String
        @Input get() = project.klibModuleName(baseKlibName)

    @get:Internal
    val outputFile: File
        get() = outputFileProvider.get()

    // Inputs and outputs.

    @OutputFile
    val outputFileProvider: Provider<File> =
        project.provider { destinationDir.get().resolve(outputFileName) }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val defFile: File
        get() = settings.defFileProperty.get()

    val packageName: String?
        @Optional @Input get() = settings.packageName

    val compilerOpts: List<String>
        @Input get() = settings.compilerOpts

    val linkerOpts: List<String>
        @Input get() = settings.linkerOpts

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val headers: FileCollection
        get() = settings.headers

    val allHeadersDirs: Set<File>
        @Input get() = settings.includeDirs.allHeadersDirs.files

    val headerFilterDirs: Set<File>
        @Input get() = settings.includeDirs.headerFilterDirs.files

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val libraries: FileCollection
        get() = settings.dependencyFiles.filterOutPublishableInteropLibs(project)

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

            libraries.files.filterKlibsPassedToCompiler(project).forEach { library ->
                addArg("-library", library.absolutePath)
            }

            addArgs("-compiler-option", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            if (project.konanVersion.isAtLeast(1, 4, 0)) {
                addArg("-Xmodule-name", moduleName)
            }

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KotlinNativeCInteropRunner(project).run(args)
    }
}
