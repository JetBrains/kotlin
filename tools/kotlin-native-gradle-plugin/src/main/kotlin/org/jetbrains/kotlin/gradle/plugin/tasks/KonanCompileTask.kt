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

import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File

enum class Produce(val cliOption: String, val kind: CompilerOutputKind) {
    PROGRAM("program", CompilerOutputKind.PROGRAM),
    DYNAMIC("dynamic", CompilerOutputKind.DYNAMIC),
    FRAMEWORK("framework", CompilerOutputKind.FRAMEWORK),
    LIBRARY("library", CompilerOutputKind.LIBRARY),
    BITCODE("bitcode", CompilerOutputKind.BITCODE)
}

/**
 * A task compiling the target executable/library using Kotlin/Native compiler
 */
abstract class KonanCompileTask: KonanBuildingTask(), KonanCompileSpec {

    @Internal override val toolRunner = KonanCompilerRunner(project)

    abstract val produce: Produce
        @Internal get

    // Output artifact --------------------------------------------------------

    override val artifactSuffix: String
        @Internal get() = produce.kind.suffix(konanTarget)

    override val artifactPrefix: String
        @Internal get() = produce.kind.prefix(konanTarget)

    // Multiplatform support --------------------------------------------------

    @Input var commonSourceSets = listOf("main")

    @Internal var enableMultiplatform = false

    internal val commonSrcFiles_ = mutableSetOf<FileCollection>()
    val commonSrcFiles: Collection<FileCollection>
        @InputFiles get() = if (enableMultiplatform) commonSrcFiles_ else emptyList()

    // Other compilation parameters -------------------------------------------

    protected val srcFiles_ = mutableSetOf<FileCollection>()
    val srcFiles: Collection<FileCollection>
        @InputFiles get() = srcFiles_.takeIf { !it.isEmpty() } ?: listOf(project.konanDefaultSrcFiles)

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
                addFileArgs("-library", project.files(it.absolutePath))
            }

        }
        addFileArgs("-library", libraries.files)
        addArgs("-library", libraries.namedKlibs)
        addArgs("-library", libraries.artifacts.map { it.artifact.canonicalPath })

        addFileArgs("-nativelibrary", nativeLibraries)
        addArg("-produce", produce.cliOption)

        addListArg("-linkerOpts", linkerOpts)

        addArgIfNotNull("-target", konanTarget.visibleName)
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)
        addArgIfNotNull("-entry", entryPoint)

        addKey("-g", enableDebug)
        addKey("-nostdlib", noStdLib)
        addKey("-nomain", noMain)
        addKey("-opt", enableOptimizations)
        addKey("-ea", enableAssertions)
        addKey("--time", measureTime)
        addKey("-nodefaultlibs", noDefaultLibs)
        addKey("-Xmulti-platform", enableMultiplatform)

        addAll(extraOpts)

        listOf(srcFiles, commonSrcFiles).forEach {
            it.flatMap { it.files }.filter { it.name.endsWith(".kt") }.mapTo(this) { it.canonicalPath }
        }
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
}

open class KonanCompileProgramTask: KonanCompileTask() {
    override val produce: Produce  get() = Produce.PROGRAM
}

open class KonanCompileDynamicTask: KonanCompileTask() {
    override val produce: Produce  get() = Produce.DYNAMIC

    val headerFile: File
        @OutputFile get() = destinationDir.resolve("$artifactPrefix${artifactName}_api.h")
}

open class KonanCompileFrameworkTask: KonanCompileTask() {
    override val produce: Produce  get() = Produce.FRAMEWORK

    override val artifact
        @OutputDirectory get() = super.artifact
}

open class KonanCompileLibraryTask: KonanCompileTask() {
    override val produce: Produce  get() = Produce.LIBRARY
}

open class KonanCompileBitcodeTask: KonanCompileTask() {
    override val produce: Produce  get() = Produce.BITCODE
}
