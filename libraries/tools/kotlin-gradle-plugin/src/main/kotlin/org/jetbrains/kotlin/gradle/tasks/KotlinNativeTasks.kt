package org.jetbrains.kotlin.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.compilerRunner.KonanCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KonanInteropRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.defaultSourceSetName
import org.jetbrains.kotlin.gradle.plugin.mpp.isMainCompilation
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
    toPath().startsWith(project.file(project.konanHome).toPath())

// We need to filter out interop duplicates because we create copy of them for IDE.
// TODO: Remove this after interop rework.
private fun FileCollection.filterOutPublishableInteropLibs(project: Project): FileCollection {
    val libDirectories = project.rootProject.allprojects.map { it.buildDir.resolve("libs").absoluteFile.toPath() }
    return filter { file ->
        !(file.name.contains("-cinterop-") && libDirectories.any { file.toPath().startsWith(it) })
    }
}

// endregion

open class KotlinNativeCompile : AbstractCompile() {

    init {
        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }

    @Internal
    lateinit var compilation: KotlinNativeCompilation

    // region inputs/outputs
    @Input
    lateinit var outputKind: CompilerOutputKind

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

    @Input
    var optimized = false
    @Input
    var debuggable = true

    val processTests
        @Input get() = compilation.isTestCompilation

    val target: String
        @Input get() = compilation.target.konanTarget.name

    val entryPoint: String?
        @Optional @Input get() = compilation.entryPoint

    val linkerOpts: List<String>
        @Input get() = compilation.linkerOpts

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
    private inner class NativeCompilerOpts : KotlinCommonToolOptions {
        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false

        // Delegate for compilations's extra options.
        override var freeCompilerArgs: List<String>
            get() = compilation.extraOpts
            set(value) { compilation.extraOpts = value.toMutableList() }
    }

    @Internal val kotlinOptions: KotlinCommonToolOptions = NativeCompilerOpts()

    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    fun kotlinOptions(fn: Closure<*>) {
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
        val baseName = if (compilation.isMainCompilation) project.name else compilation.name
        var filename = "$prefix$baseName$suffix"
        if (outputKind in listOf(FRAMEWORK, STATIC, DYNAMIC) || outputKind == PROGRAM && konanTarget == KonanTarget.WASM32) {
            filename = filename.replace('-', '_')
        }

        destinationDir.resolve(filename)
    }

    // endregion
    @Internal
    val compilerPluginOptions = CompilerPluginOptions()

    val compilerPluginCommandLine
        @Input get()= compilerPluginOptions.arguments

    @Optional @InputFiles
    var compilerPluginClasspath: FileCollection? = null

    val serializedCompilerArguments: List<String>
        @Internal get() = buildCommonArgs()

    val defaultSerializedCompilerArguments: List<String>
        @Internal get() = buildCommonArgs(true)

    private fun buildCommonArgs(defaultsOnly: Boolean = false) = mutableListOf<String>().apply {

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

    private fun buildArgs(defaultsOnly: Boolean = false) = mutableListOf<String>().apply {
        addKey("-opt", optimized)
        addKey("-g", debuggable)
        addKey("-ea", debuggable)
        addKey("-tr", processTests)

        addArg("-target", target)
        addArg("-p", outputKind.name.toLowerCase())
        addArgIfNotNull("-entry", entryPoint)

        if (!defaultsOnly) {
            addArg("-o", outputFile.get().absolutePath)

            // Libraries.
            libraries.files.filter {
                // Support only klib files for now.
                it.extension == "klib" && !it.providedByCompiler(project)
            }.forEach { library ->
                addArg("-l", library.absolutePath)
            }
        }

        val friends = friendModule?.files
        if (friends != null && friends.isNotEmpty()) {
            addArg("-friend-modules", friends.map { it.absolutePath }.joinToString(File.pathSeparator))
        }

        addListArg("-linker-options", linkerOpts)

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

open class CInteropProcess: DefaultTask() {

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

            addFileArgs("-h", headers)

            compilerOpts.forEach {
                addArg("-copt", it)
            }

            linkerOpts.forEach {
                addArg("-lopt", it)
            }

            libraries.files.filter {
                // Support only klib files for now.
                it.extension == "klib" && !it.providedByCompiler(project)
            }.forEach { library ->
                addArg("-l", library.absolutePath)
            }

            addArgs("-copt", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KonanInteropRunner(project).run(args)
    }
}
