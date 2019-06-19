/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.gradle.plugin.model.KonanModelArtifact
import org.jetbrains.kotlin.gradle.plugin.model.KonanModelArtifactImpl
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * A task compiling the target executable/library using Kotlin/Native compiler
 */
abstract class KonanCompileTask: KonanBuildingTask(), KonanCompileSpec {

    @Internal override val toolRunner =
        KonanCompilerRunner(project, project.konanExtension.jvmArgs)

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

    internal val commonSrcFiles_ = mutableSetOf<FileCollection>()
    val commonSrcFiles: Collection<FileCollection>
        @Internal get() = if (enableMultiplatform) commonSrcFiles_ else emptyList()

    // Other compilation parameters -------------------------------------------

    protected val srcFiles_ = mutableSetOf<FileCollection>()
    val srcFiles: Collection<FileCollection>
        @Internal get() = srcFiles_.takeIf { !it.isEmpty() } ?: listOf(project.konanDefaultSrcFiles)

    val allSources: Collection<FileCollection>
        @InputFiles get() = listOf(srcFiles, commonSrcFiles).flatten()

    private val allSourceFiles: List<File>
        @Internal get() = allSources
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

    protected fun directoryToKt(dir: Any) = project.fileTree(dir).apply {
        include("**/*.kt")
        exclude { it.file.startsWith(project.buildDir) }
    }

    // Command line  ------------------------------------------------------------

    override fun buildArgs() = mutableListOf<String>().apply {
        addArg("-output", artifact.canonicalPath)

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

        addFileArgs("-native-library", nativeLibraries)
        addArg("-produce", produce.name.toLowerCase())
        linkerOpts.forEach {
            addArg("-linker-option", it)
        }

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
        addKey("-nodefaultlibs", noDefaultLibs)
        addKey("-Xmulti-platform", enableMultiplatform)

        if (libraries.friends.isNotEmpty())
            addArg("-friend-modules", libraries.friends.joinToString(File.pathSeparator))

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

    // region IDE model
    override fun toModelArtifact(): KonanModelArtifact {
        val repos = libraries.repos
        val resolver = defaultResolver(
            repos.map { it.absolutePath },
            konanTarget,
            Distribution(konanHomeOverride = project.konanHome)
        )

        return KonanModelArtifactImpl(
                artifactName,
                artifact,
                produce,
                konanTarget.name,
                name,
                allSources.filterIsInstance(ConfigurableFileTree::class.java).map { it.dir },
                allSourceFiles,
                libraries.asFiles(resolver),
                repos.toList()
        )
    }
    // endregion
}

open class KonanCompileProgramTask: KonanCompileTask() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.PROGRAM

    var runTask: Exec? = null

    inner class RunArgumentProvider(): CommandLineArgumentProvider {
        override fun asArguments() = project.findProperty("runArgs")?.let {
            it.toString().split(' ')
        } ?: emptyList()
    }

    // Create tasks to run supported executables.
    override fun init(config: KonanBuildingConfig<*>, destinationDir: File, artifactName: String, target: KonanTarget) {
        super.init(config, destinationDir, artifactName, target)
        if (!isCrossCompile && !project.hasProperty("konanNoRun")) {
            runTask = project.tasks.create("run${artifactName.capitalize()}", Exec::class.java).apply {
                group = "run"
                dependsOn(this@KonanCompileProgramTask)
                val artifactPathClosure = object : Closure<String>(this) {
                    override fun call() = artifactPath
                }
                // Use GString to evaluate a path to the artifact lazily thus allow changing it at configuration phase.
                val lazyArtifactPath = GStringImpl(arrayOf(artifactPathClosure), arrayOf(""))
                executable(lazyArtifactPath)
                // Add values passed in the runArgs project property as arguments.
                argumentProviders.add(RunArgumentProvider())
            }
        }
    }

}

open class KonanCompileDynamicTask: KonanCompileTask() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.DYNAMIC

    val headerFile: File
        @OutputFile get() = destinationDir.resolve("$artifactPrefix${artifactName}_api.h")
}

open class KonanCompileFrameworkTask: KonanCompileTask() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.FRAMEWORK

    override val artifact
        @OutputDirectory get() = super.artifact
}

open class KonanCompileLibraryTask: KonanCompileTask() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.LIBRARY
}

open class KonanCompileBitcodeTask: KonanCompileTask() {
    override val produce: CompilerOutputKind get() = CompilerOutputKind.BITCODE
}
