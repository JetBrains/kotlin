/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * A task compiling the target executable/library using Kotlin/Native compiler
 */
abstract class KonanCompileTask @Inject constructor(private val layout: ProjectLayout):  KonanBuildingTask(), KonanCompileSpec {

    abstract val produce: CompilerOutputKind
        @Internal get

    // Output artifact --------------------------------------------------------

    override val artifactSuffix: String
        @Internal get() = produce.suffix(konanTarget)

    override val artifactPrefix: String
        @Internal get() = produce.prefix(konanTarget)

    // Multiplatform support --------------------------------------------------

    @Input var commonSourceSets = listOf("main")

    @Internal var enableMultiplatform = false

    private val commonSrcFiles_ = mutableSetOf<FileCollection>()
    val commonSrcFiles: Collection<FileCollection>
        @Internal get() = if (enableMultiplatform) commonSrcFiles_ else emptyList()

    // Other compilation parameters -------------------------------------------

    private val srcFiles_ = mutableSetOf<FileCollection>()
    private val defaultFiles = project.konanDefaultSrcFiles
    val srcFiles: Collection<FileCollection>
        @Internal get() {
            return srcFiles_.takeIf { !it.isEmpty() } ?: listOf(defaultFiles)
        }

    val allSources: Collection<FileCollection>
        @InputFiles @PathSensitive(PathSensitivity.RELATIVE) get() = listOf(srcFiles, commonSrcFiles).flatten()

    private val allSourceFiles: List<File>
        get() = allSources
                .flatMap { it.files }
                .filter { it.name.endsWith(".kt") }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val nativeLibraries = mutableSetOf<FileCollection>()

    @Input val linkerOpts = mutableListOf<String>()

    @Input var enableDebug = project.findProperty("enableDebug")?.toString()?.toBoolean()
            ?: project.environmentVariables.debuggingSymbols

    @Input var noStdLib            = false
    @Input var noMain              = false
    @Input var noPack: Boolean     = false
    @Input var enableOptimizations = project.environmentVariables.enableOptimizations
    @Input var enableAssertions    = false

    @Optional @Input var entryPoint: String? = null

    @Console var measureTime       = false

    private val konanBuildRoot = project.konanBuildRoot

    @Internal
    val konanExtension = project.konanExtension

    val languageVersion : String?
        @Optional @Input get() = konanExtension.languageVersion
    val apiVersion      : String?
        @Optional @Input get() = konanExtension.apiVersion

    @Input var buildNumber = project.properties.get("kotlinVersion") ?: error("kotlinVersion property is not specified in the project")

    /**
     * Is the two-stage compilation enabled.
     *
     * In regular (one-stage) compilation, sources are directly compiled into a final native binary.
     * In two-stage compilation, sources are compiled into a klib first and then a final native binary is produced from this klib.
     */
    @get:Input
    abstract val enableTwoStageCompilation: Boolean

    protected fun directoryToKt(dir: Any) = project.fileTree(dir).apply {
        include("**/*.kt")
        exclude { it.file.startsWith(layout.buildDirectory.get().asFile) }
    }

    // Command line  ------------------------------------------------------------

    // Exclude elements matching the predicate.
    private fun List<String>.excludeFlags(predicate: (String) -> Boolean) = filterNot(predicate)

    // Exclude the listed elements.
    private fun List<String>.excludeFlags(vararg keys: String) = keys.toSet().let { keysToExclude ->
        excludeFlags { it in keysToExclude }
    }

    // Exclude the arguments passed by the given keys.
    // E.g. if the list contains the following elements: ["-l", "foo", "-r", "bar"],
    // call exclude("-r") returns the following list: ["-l", "foo"].
    private fun List<String>.excludeArguments(vararg args: String): List<String> {
        val argsToExclude = args.toSet()
        val xPrefixesToExclude = argsToExclude.filter { it.startsWith("-X") }.map { "$it=" }
        val result = mutableListOf<String>()

        var i = 0
        while (i < size) {
            val key = this[i]
            when {
                key in argsToExclude -> {
                    // Skip the key and the following arg.
                    i++
                }
                // Support args passed as -X<arg>=<value>.
                xPrefixesToExclude.any { key.startsWith(it) } -> { /* Skip the key. */ }
                else -> result += key
            }
            i++
        }
        return result
    }

    private fun firstStageExtraOpts() = extraOpts
        .excludeArguments("-Xpartial-linkage", "-Xpartial-linkage-loglevel")

    // Don't include the -Xemit-lazy-objc-header and -language-version flags into
    // the second stage because this stage have no sources.
    private fun secondStageExtraOpts() = extraOpts
        .excludeArguments("-Xemit-lazy-objc-header", "-language-version")

    /** Args passed to the compiler at the first stage of two-stage compilation (klib building). */
    protected fun buildFirstStageArgs(klibPath: String) = mutableListOf<String>().apply {
        addArg("-output", klibPath)
        addArg("-produce", CompilerOutputKind.LIBRARY.name.lowercase(Locale.getDefault()))

        addAll(buildCommonArgs())

        addAll(firstStageExtraOpts())

        allSourceFiles.mapTo(this) { it.absolutePath }
        commonSrcFiles
            .flatMap { it.files }
            .mapTo(this) { "-Xcommon-sources=${it.absolutePath}" }
    }

    /** Args passed to the compiler at the second stage of two-stage compilation (producing a final binary from the klib).  */
    protected fun buildSecondStageArgs(klibPath: String) = mutableListOf<String>().apply {
        addArg("-output", artifact.canonicalPath)
        addArg("-produce", produce.name.lowercase(Locale.getDefault()))
        addArgIfNotNull("-entry", entryPoint)

        addAll(buildCommonArgs())

        addFileArgs("-native-library", nativeLibraries)
        linkerOpts.forEach {
            addArg("-linker-option", it)
        }

        addAll(secondStageExtraOpts())

        add("-Xinclude=${klibPath}")
    }

    /** Args passed to the compiler at both stages of the two-stage compilation and during the singe-stage compilation. */
    protected open fun buildCommonArgs() = mutableListOf<String>().apply {
        if (platformConfigurationFiles.files.isNotEmpty()) {
            platformConfigurationFiles.files.filter { it.name.endsWith(".klib") }.forEach {
                // The library's directory is added in libraries.repos.
                addArg("-library", it.absolutePath)
            }
        }
        addFileArgs("-library", libraries.klibFiles)
        addArgs("-library", libraries.artifacts.map { it.artifact.canonicalPath })

        addArgIfNotNull("-target", konanTarget.visibleName)
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)
        addArgIfNotNull("-entry", entryPoint)

        addKey("-g", enableDebug)
        addKey("-nostdlib", noStdLib)
        addKey("-nomain", noMain)
        addKey("-opt", enableOptimizations)
        addKey("-ea", enableAssertions)
        addKey("-Xtime", measureTime)
        addKey("-Xprofile-phases", measureTime)
        addKey("-no-default-libs", noDefaultLibs)
        addKey("-no-endorsed-libs", noEndorsedLibs)
        addKey("-Xmulti-platform", enableMultiplatform)

        if (libraries.friends.isNotEmpty())
            addArg("-friend-modules", libraries.friends.joinToString(File.pathSeparator))
    }

    /** Args passed to the compiler if the two-stage compilation is disabled. */
    fun buildSingleStageArgs() = mutableListOf<String>().apply {
        addArg("-output", artifact.canonicalPath)
        addArg("-produce", produce.name.lowercase(Locale.getDefault()))
        addArgIfNotNull("-entry", entryPoint)

        addAll(buildCommonArgs())

        addFileArgs("-native-library", nativeLibraries)

        linkerOpts.forEach {
            addArg("-linker-option", it)
        }

        if (produce != CompilerOutputKind.LIBRARY) {
            add("-Xpartial-linkage=enable")
            add("-Xpartial-linkage-loglevel=error")
        }

        addAll(extraOpts)

        allSourceFiles.mapTo(this) { it.absolutePath }
        commonSrcFiles
            .flatMap { it.files }
            .mapTo(this) { "-Xcommon-sources=${it.absolutePath}" }
    }

    // region DSL.

    // DSL. Input/output files.

    override fun srcDir(dir: Any) {
        srcFiles_.add(directoryToKt(dir))
    }
    override fun srcFiles(vararg files: Any) {
        srcFiles_.add(project.files(files))
    }
    override fun srcFiles(files: Collection<Any>) = srcFiles(*files.toTypedArray())

    // DSL. Native libraries.

    override fun nativeLibrary(lib: Any) = nativeLibraries(lib)
    override fun nativeLibraries(vararg libs: Any) {
        nativeLibraries.add(project.files(*libs))
    }
    override fun nativeLibraries(libs: FileCollection) {
        nativeLibraries.add(libs)
    }

    // DSL. Multiplatform projects.

    override fun enableMultiplatform(flag: Boolean) {
        enableMultiplatform = flag
    }

    @Deprecated("Use commonSourceSets instead", ReplaceWith("commonSourceSets(sourceSetName)"))
    override fun commonSourceSet(sourceSetName: String) {
        commonSourceSets = listOf(sourceSetName)
        enableMultiplatform(true)
    }

    override fun commonSourceSets(vararg sourceSetNames: String) {
        commonSourceSets = sourceSetNames.toList()
        enableMultiplatform(true)
    }

    override fun commonSrcDir(dir: Any) {
        commonSrcFiles_.add(directoryToKt(dir))
    }

    override fun commonSrcFiles(vararg files: Any) {
        commonSrcFiles_.add(project.files(files))
    }

    override fun commonSrcFiles(files: Collection<Any>) = commonSrcFiles(*files.toTypedArray())


    // DSL. Other parameters.

    override fun linkerOpts(values: List<String>) = linkerOpts(*values.toTypedArray())
    override fun linkerOpts(vararg values: String) {
        linkerOpts.addAll(values)
    }

    override fun enableDebug(flag: Boolean) {
        enableDebug = flag
    }

    override fun noStdLib(flag: Boolean) {
        noStdLib = flag
    }

    override fun noMain(flag: Boolean) {
        noMain = flag
    }

    override fun noPack(flag: Boolean) {
        noPack = flag
    }

    override fun enableOptimizations(flag: Boolean) {
        enableOptimizations = flag
    }

    override fun enableAssertions(flag: Boolean) {
        enableAssertions = flag
    }

    override fun entryPoint(entryPoint: String) {
        this.entryPoint = entryPoint
    }

    override fun measureTime(flag: Boolean) {
        measureTime = flag
    }
    // endregion

    @get:Internal
    val isolatedClassLoadersService = KonanCliRunnerIsolatedClassLoadersService.attachingToTask(this)

    override fun run() {
        destinationDir.mkdirs()
        if (dumpParameters) {
            dumpProperties(this)
        }
        val toolRunner = KonanCliCompilerRunner(project, isolatedClassLoadersService, project.konanExtension.jvmArgs)
        if (enableTwoStageCompilation) {
            logger.info("Start two-stage compilation")
            val intermediateDir = konanBuildRoot
                .resolve("intermediate")
                .targetSubdir(konanTarget)
                .apply { mkdirs() }
            val klibPrefix = CompilerOutputKind.LIBRARY.prefix(konanTarget)
            val klibSuffix = CompilerOutputKind.LIBRARY.suffix(konanTarget)
            val intermediateKlib = intermediateDir.resolve("$klibPrefix$artifactName$klibSuffix").absolutePath
            logger.info("Start first stage")
            toolRunner.run(buildFirstStageArgs(intermediateKlib))
            logger.info("Start second stage")
            toolRunner.run(buildSecondStageArgs(intermediateKlib))
        } else {
            toolRunner.run(buildSingleStageArgs())
        }
    }
}

abstract class KonanCompileNativeBinary @Inject constructor(layout: ProjectLayout) : KonanCompileTask(layout) {
    @Input
    override var enableTwoStageCompilation: Boolean = false
}

abstract class KonanCompileProgramTask @Inject constructor(layout: ProjectLayout): KonanCompileNativeBinary(layout) {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.PROGRAM

    @Internal
    var runTask: TaskProvider<Exec>? = null

    inner class RunArgumentProvider: CommandLineArgumentProvider {
        override fun asArguments() = project.findProperty("runArgs")?.let {
            it.toString().split(' ')
        } ?: emptyList()
    }
}

abstract class KonanCompileDynamicTask @Inject constructor(layout: ProjectLayout): KonanCompileNativeBinary(layout) {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.DYNAMIC

    val headerFile: File
        @OutputFile get() = destinationDir.resolve("$artifactPrefix${artifactName}_api.h")
}

abstract class KonanCompileFrameworkTask @Inject constructor(layout: ProjectLayout): KonanCompileNativeBinary(layout) {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.FRAMEWORK

    override val artifact
        @OutputDirectory get() = super.artifact
}

@CacheableTask
abstract class KonanCompileLibraryTask @Inject constructor(layout: ProjectLayout) : KonanCompileTask(layout) {
    init {
        this.notCompatibleWithConfigurationCache("project references stored")
    }

    override val artifact: File
        @Internal get() = destinationDir.resolve(artifactFullName)

    val artifactFile: File?
        @Optional @OutputFile get() = if (!noPack) artifact else null

    val artifactDirectory: File?
        @Optional @OutputDirectory get() = if (noPack) artifact else null

    override val artifactSuffix: String
        @Internal get() = if (!noPack) produce.suffix(konanTarget) else ""

    override fun buildCommonArgs() = super.buildCommonArgs().apply {
        addKey("-nopack", noPack)
    }

    override val produce: CompilerOutputKind get() = CompilerOutputKind.LIBRARY
    override val enableTwoStageCompilation: Boolean = false
}

abstract class KonanCompileBitcodeTask @Inject constructor(layout: ProjectLayout): KonanCompileNativeBinary(layout) {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.BITCODE
}
