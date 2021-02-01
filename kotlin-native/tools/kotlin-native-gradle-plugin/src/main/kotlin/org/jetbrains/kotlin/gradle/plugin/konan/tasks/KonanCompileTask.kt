/*
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.tasks

import groovy.lang.Closure
import org.codehaus.groovy.runtime.GStringImpl
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * A task compiling the target executable/library using Kotlin/Native compiler
 */
abstract class KonanCompileTask: KonanBuildingTask(), KonanCompileSpec {

    @get:Internal
    override val toolRunner = KonanCompilerRunner(project, project.konanExtension.jvmArgs)

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
    val srcFiles: Collection<FileCollection>
        @Internal get() = srcFiles_.takeIf { !it.isEmpty() } ?: listOf(project.konanDefaultSrcFiles)

    val allSources: Collection<FileCollection>
        @InputFiles get() = listOf(srcFiles, commonSrcFiles).flatten()

    private val allSourceFiles: List<File>
        get() = allSources
                .flatMap { it.files }
                .filter { it.name.endsWith(".kt") }

    @InputFiles val nativeLibraries = mutableSetOf<FileCollection>()

    @Input val linkerOpts = mutableListOf<String>()

    @Input var enableDebug = project.findProperty("enableDebug")?.toString()?.toBoolean()
            ?: project.environmentVariables.debuggingSymbols

    @Input var noStdLib            = false
    @Input var noMain              = false
    @Input var enableOptimizations = project.environmentVariables.enableOptimizations
    @Input var enableAssertions    = false

    @Optional @Input var entryPoint: String? = null

    @Console var measureTime       = false

    val languageVersion : String?
        @Optional @Input get() = project.konanExtension.languageVersion
    val apiVersion      : String?
        @Optional @Input get() = project.konanExtension.apiVersion

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
        exclude { it.file.startsWith(project.buildDir) }
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

    // Don't include coverage flags into the first stage because they are not supported when compiling a klib.
    private fun firstStageExtraOpts() = extraOpts
        .excludeFlags("-Xcoverage")
        .excludeArguments("-Xcoverage-file", "-Xlibrary-to-cover")

    // Don't include the -Xemit-lazy-objc-header flag into
    // the second stage because this stage have no sources.
    private fun secondStageExtraOpts() = extraOpts
        .excludeArguments("-Xemit-lazy-objc-header")

    /** Args passed to the compiler at the first stage of two-stage compilation (klib building). */
    protected fun buildFirstStageArgs(klibPath: String) = mutableListOf<String>().apply {
        addArg("-output", klibPath)
        addArg("-produce", CompilerOutputKind.LIBRARY.name.toLowerCase())

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
        addArg("-produce", produce.name.toLowerCase())
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
    protected fun buildCommonArgs() = mutableListOf<String>().apply {
        addArgs("-repo", libraries.repos.map { it.canonicalPath })

        if (platformConfiguration.files.isNotEmpty()) {
            platformConfiguration.files.filter { it.name.endsWith(".klib") }.forEach {
                // The library's directory is added in libraries.repos.
                addArg("-library", it.nameWithoutExtension)
            }
        }
        addFileArgs("-library", libraries.files)
        addArgs("-library", libraries.namedKlibs)
        // The library's directory is added in libraries.repos.
        addArgs("-library", libraries.artifacts.map { it.artifact.nameWithoutExtension })

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
        addArg("-produce", produce.name.toLowerCase())
        addArgIfNotNull("-entry", entryPoint)

        addAll(buildCommonArgs())

        addFileArgs("-native-library", nativeLibraries)

        linkerOpts.forEach {
            addArg("-linker-option", it)
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

    internal fun commonSrcDir(dir: Any) {
        commonSrcFiles_.add(directoryToKt(dir))
    }

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

    override fun run() {
        destinationDir.mkdirs()
        if (dumpParameters) {
            dumpProperties(this)
        }
        if (enableTwoStageCompilation) {
            logger.info("Start two-stage compilation")
            val intermediateDir = project.konanBuildRoot
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

abstract class KonanCompileNativeBinary: KonanCompileTask() {
    @Input
    override var enableTwoStageCompilation: Boolean = false
}

open class KonanCompileProgramTask: KonanCompileNativeBinary() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.PROGRAM

    @Internal
    var runTask: TaskProvider<Exec>? = null

    inner class RunArgumentProvider: CommandLineArgumentProvider {
        override fun asArguments() = project.findProperty("runArgs")?.let {
            it.toString().split(' ')
        } ?: emptyList()
    }
}

open class KonanCompileDynamicTask: KonanCompileNativeBinary() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.DYNAMIC

    val headerFile: File
        @OutputFile get() = destinationDir.resolve("$artifactPrefix${artifactName}_api.h")
}

open class KonanCompileFrameworkTask: KonanCompileNativeBinary() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.FRAMEWORK

    override val artifact
        @OutputDirectory get() = super.artifact
}

open class KonanCompileLibraryTask: KonanCompileTask() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.LIBRARY
    override val enableTwoStageCompilation: Boolean = false
}

open class KonanCompileBitcodeTask: KonanCompileNativeBinary() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.BITCODE
}
