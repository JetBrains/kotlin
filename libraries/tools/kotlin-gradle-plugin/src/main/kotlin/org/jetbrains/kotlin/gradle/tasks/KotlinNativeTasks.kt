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
import org.jetbrains.kotlin.compilerRunner.KonanCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KonanInteropRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

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

internal fun MutableList<String>.addListArg(parameter: String, values: List<String>) {
    if (values.isNotEmpty()) {
        addArg(parameter, values.joinToString(separator = " "))
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
abstract class AbstractKotlinNativeCompile : AbstractCompile(), KotlinCompile<KotlinCommonOptions> {

    init {
        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }

    abstract val compilation: KotlinNativeCompilation

    // region inputs/outputs
    @get:Input
    abstract val outputKind: CompilerOutputKind

    @get:Input
    abstract val optimized: Boolean

    @get:Input
    abstract val debuggable: Boolean

    abstract val baseName: String

    // Inputs and outputs
    @InputFiles
    @SkipWhenEmpty
    override fun getSource(): FileTree = project.files(compilation.allSources).asFileTree

    private val commonSources: FileCollection
        // Already taken into account in getSources method.
        get() = project.files(compilation.commonSources).asFileTree

    val libraries: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles.filterOutPublishableInteropLibs(project)

    private val friendModule: FileCollection?
        get() = compilation.friendCompilation?.output?.allOutputs

    override fun getClasspath(): FileCollection = libraries
    override fun setClasspath(configuration: FileCollection?) {
        throw UnsupportedOperationException("Setting classpath directly is unsupported.")
    }

    val processTests
        @Input get() = compilation.isTestCompilation

    val target: String
        @Input get() = compilation.target.konanTarget.name

    // TODO: rework.
    val additionalCompilerOptions: Collection<String>
        @Input get() = compilation.extraOpts

    // region Language settings imported from a SourceSet.
    val languageSettings: LanguageSettingsBuilder?
        @Internal get() = project.kotlinExtension.sourceSets.findByName(compilation.defaultSourceSetName)?.languageSettings

    val languageVersion: String?
        @Optional @Input get() = languageSettings?.languageVersion

    val apiVersion: String?
        @Optional @Input get() = languageSettings?.apiVersion

    val progressiveMode: Boolean
        @Input get() = languageSettings?.progressiveMode ?: false

    val enabledLanguageFeatures: Set<String>
        @Input get() = languageSettings?.enabledLanguageFeatures ?: emptySet()

    val experimentalAnnotationsInUse: Set<String>
        @Input get() = languageSettings?.experimentalAnnotationsInUse.orEmpty()
    // endregion.

    // region DSL for compiler options
    private inner class NativeCompilerOpts : KotlinCommonOptions {
        override var apiVersion: String?
            get() = languageSettings?.apiVersion
            set(value) { languageSettings!!.apiVersion = value }

        override var languageVersion: String?
            get() = this@AbstractKotlinNativeCompile.languageVersion
            set(value) { languageSettings!!.languageVersion = value }

        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false

        // TODO: deprecate extraOptions because now we have a uniform way to access these parameters.
        // Delegate for compilations's extra options.
        override var freeCompilerArgs: List<String>
            get() = compilation.extraOpts
            set(value) {
                compilation.extraOpts = value.toMutableList()
            }
    }

    @Internal
    override val kotlinOptions: KotlinCommonOptions = NativeCompilerOpts()

    override fun kotlinOptions(fn: KotlinCommonOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    override fun kotlinOptions(fn: Closure<*>) {
        fn.delegate = kotlinOptions
        fn.call()
    }

    // endregion.

    val kotlinNativeVersion: String
        @Input get() = project.konanVersion.toString()

    // OutputFile is located under the destinationDir, so there is no need to register it as a separate output.
    @Internal
    val outputFile: Provider<File> = project.provider {
        val konanTarget = compilation.target.konanTarget

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

    val serializedCompilerArguments: List<String>
        @Internal get() = buildCommonArgs()

    val defaultSerializedCompilerArguments: List<String>
        @Internal get() = buildCommonArgs(true)

    private fun buildCommonArgs(defaultsOnly: Boolean = false): List<String> = mutableListOf<String>().apply {

        add("-Xmulti-platform")

        // Language features.
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)
        addKey("-progressive", progressiveMode)
        enabledLanguageFeatures.forEach { featureName ->
            add("-XXLanguage:+$featureName")
        }
        experimentalAnnotationsInUse.forEach { annotationName ->
            add("-Xuse-experimental=$annotationName")
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

        if (!defaultsOnly) {
            addAll(additionalCompilerOptions)
        }
    }

    protected open fun buildArgs(defaultsOnly: Boolean = false): List<String> = mutableListOf<String>().apply {
        addKey("-opt", optimized)
        addKey("-g", debuggable)
        addKey("-ea", debuggable)
        addKey("-tr", processTests)

        addArg("-target", target)
        addArg("-p", outputKind.name.toLowerCase())

        if (!defaultsOnly) {
            addArg("-o", outputFile.get().absolutePath)

            // Libraries.
            libraries.files.filterExternalKlibs(project).forEach { library ->
                addArg("-l", library.absolutePath)
            }
        }

        val friends = friendModule?.files
        if (friends != null && friends.isNotEmpty()) {
            addArg("-friend-modules", friends.map { it.absolutePath }.joinToString(File.pathSeparator))
        }

        addAll(buildCommonArgs(defaultsOnly))

        // Sources.
        addAll(getSource().map { it.absolutePath })
        if (!commonSources.isEmpty) {
            add("-Xcommon-sources=${commonSources.map { it.absolutePath }.joinToString(separator = ",")}")
        }
    }

    @TaskAction
    override fun compile() {
        val output = outputFile.get()
        output.parentFile.mkdirs()
        KonanCompilerRunner(project).run(buildArgs())
    }
}

/**
 * A task producing a klibrary from a compilation.
 */
open class KotlinNativeCompile : AbstractKotlinNativeCompile() {
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
}

/**
 * A task producing a final binary from a compilation.
 */
open class KotlinNativeLink : AbstractKotlinNativeCompile() {
    @Internal
    lateinit var binary: NativeBinary

    @get:Internal
    override val compilation: KotlinNativeCompilation
        get() = binary.compilation

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

    @get:Optional
    @get:Input
    val entryPoint: String?
        get() = (binary as? Executable)?.entryPoint

    @get:Input
    val linkerOpts: List<String>
        get() = binary.linkerOpts

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

    @get:Input
    val freeCompilerArgs: List<String>
        get() = binary.freeCompilerArgs

    override fun buildArgs(defaultsOnly: Boolean): List<String> {
        val superArgs = super.buildArgs(defaultsOnly)
        return mutableListOf<String>().apply {
            addAll(superArgs)
            addArgIfNotNull("-entry", entryPoint)
            when (embedBitcode) {
                Framework.BitcodeEmbeddingMode.MARKER -> add("-Xembed-bitcode-marker")
                Framework.BitcodeEmbeddingMode.BITCODE -> add("-Xembed-bitcode")
                else -> { /* Do nothing. */ }
            }
            addListArg("-linker-options", linkerOpts)
            exportLibraries.files.filterExternalKlibs(project).forEach {
                add("-Xexport-library=${it.absolutePath}")
            }
            addKey("-Xstatic-framework", isStaticFramework)
            addAll(freeCompilerArgs)
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
}

open class CInteropProcess : DefaultTask() {

    @Internal
    lateinit var settings: DefaultCInteropSettings

    @Internal // Taken into account in the outputFileProvider property
    lateinit var destinationDir: Provider<File>

    val konanTarget: KonanTarget
        @Internal get() = settings.compilation.target.konanTarget

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

    val outputFile: File
        @Internal get() = outputFileProvider.get().asFile

    // Inputs and outputs.

    @OutputFile
    val outputFileProvider = newOutputFile().apply {
        set { destinationDir.get().resolve(outputFileName) }
    }

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
                addArg("-copt", it)
            }

            linkerOpts.forEach {
                addArg("-lopt", it)
            }

            libraries.files.filterExternalKlibs(project).forEach { library ->
                addArg("-library", library.absolutePath)
            }

            addArgs("-copt", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KonanInteropRunner(project).run(args)
    }
}
