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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.*
import java.io.File

/**
 *  We can the following:
 *
 *  konanArtifacts {
 *
 *      artifactName1 {
 *
 *          inputFiles "files" "to" "be" "compiled"
 *
 *          outputDir "path/to/output/dir"
 *
 *          library "path/to/library"
 *          library File("Library")
 *
 *          nativeLibrary "path/to/library"
 *          nativeLibrary File("Library")
 *
 *          noStdLib
 *          produce "library"|"program"|"bitcode"
 *          enableOptimization
 *
 *          linkerOpts "linker" "args"
 *          target "target"
 *
 *          languageVersion "version"
 *          apiVersion "version"
 *
 *     }
 *
 *     artifactName2 {
 *
 *          extends artifactName1
 *
 *          inputDir "someDir"
 *          outputDir "someDir"
 *     }
 *
 *  }
 */


// TODO: Make the task class nested for config with properties accessible for outer users.
open class KonanCompileTask: KonanTargetableTask() {

    companion object {
        const val COMPILER_MAIN = "org.jetbrains.kotlin.cli.bc.K2NativeKt"
    }

    val COMPILER_JVM_ARGS: List<String>
        @Internal get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")

    // Output artifact --------------------------------------------------------

    internal lateinit var artifactName: String
        @Internal get

    @Internal lateinit var outputDir: File
        internal set

    internal fun init(artifactName: String) {
        dependsOn(project.konanCompilerDownloadTask)
        this.artifactName = artifactName
        outputDir = project.file(project.konanCompilerOutputDir)
    }

    val artifactNamePath: String
        @Internal get() = "${outputDir.absolutePath}/$artifactName"

    val artifactSuffix: String
        @Internal get() = produceSuffix(produce)

    val artifactPath: String
        @Internal get() = "$artifactNamePath$artifactSuffix"

    val artifact: File
        @OutputFile get() = project.file(artifactPath)

    // Other compilation parameters -------------------------------------------

    internal val _inputFiles = mutableSetOf<FileCollection>()
    val inputFiles: Collection<FileCollection>
        @InputFiles get() = _inputFiles.takeIf { !it.isEmpty() } ?: listOf(project.konanDefaultSrcFiles)

    @InputFiles val libraries       = mutableSetOf<FileCollection>()
    @InputFiles val nativeLibraries = mutableSetOf<FileCollection>()

    @Input var produce              = "program"
        internal set

    @Internal val interops = mutableSetOf<KonanInteropConfig>()

    internal var _linkerOpts = mutableListOf<String>()
    val linkerOpts: List<String>
        @Input get() = mutableListOf<String>().apply {
            addAll(_linkerOpts)
            interops.flatMapTo(this) { it.generateStubsTask.linkerOpts }
        }

    @Input var enableDebug        = project.properties.containsKey("enableDebug") && project.properties["enableDebug"].toString().toBoolean()
        internal set
    @Input var noStdLib           = false
        internal set
    @Input var noMain             = false
        internal set
    @Input var enableOptimization = false
        internal set
    @Input var enableAssertions   = false
        internal set

    @Optional @Input var languageVersion : String? = null
        internal set
    @Optional @Input var apiVersion      : String? = null
        internal set

    @Optional @InputFile var manifest    : File? = null
        internal set

    @Input var dumpParameters: Boolean = false
    // TODO: Is there a better way to rerun tasks when the compiler version changes?
    @Input val konanVersion = project.konanVersion

    // Task action ------------------------------------------------------------

    protected fun buildArgs() = mutableListOf<String>().apply {
        addArg("-output", artifactNamePath)

        addFileArgs("-library", libraries)
        addFileArgs("-nativelibrary", nativeLibraries)
        addArg("-produce", produce)

        addListArg("-linkerOpts", linkerOpts)

        addArgIfNotNull("-target", target)
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)
        // TODO: Should we get manifests of the interops used?
        addArgIfNotNull("-manifest", manifest?.canonicalPath)

        addKey("-g", enableDebug)
        addKey("-nostdlib", noStdLib)
        addKey("-nomain", noMain)
        addKey("-opt", enableOptimization)
        addKey("-ea", enableAssertions)

        inputFiles.flatMap { it.files }.filter { it.name.endsWith(".kt") }.mapTo(this) { it.canonicalPath }
    }

    @TaskAction
    fun compile() {
        project.file(outputDir).mkdirs()

        if (dumpParameters) dumpProperties(this@KonanCompileTask)

        // TODO: Use compiler service.
        project.javaexec {
            with(it) {
                main = COMPILER_MAIN
                classpath = project.konanCompilerClasspath
                jvmArgs(COMPILER_JVM_ARGS)
                args(buildArgs().apply { logger.info("Compiler args: ${this.joinToString(separator = " ")}") })
            }
        }
    }
}

// TODO: check debug outputs
// TODO: Use +=/-= syntax for libraries and inputFiles
open class KonanCompileConfig(
        val configName: String,
        val project: Project,
        taskNamePrefix: String = "compileKonan"): Named {

    override fun getName() = configName

    val compilationTask: KonanCompileTask = project.tasks.create(
            "$taskNamePrefix${configName.capitalize()}",
            KonanCompileTask::class.java) {
        it.init(this@KonanCompileConfig.name)
        it.group = BasePlugin.BUILD_GROUP
        it.description = "Compiles the Kotlin/Native artifact '${this@KonanCompileConfig.name}'"
    }

    // DSL methods. Interop. --------------------------------------------------

    fun useInterop(interop: KonanInteropConfig) = with(compilationTask) {
        val generateStubsTask = interop.generateStubsTask
        val compileStubsTask = interop.compileStubsTask

        dependsOn(compileStubsTask)
        dependsOn(generateStubsTask)
        library(project.files(compileStubsTask.artifact))
        nativeLibrary(project.fileTree(generateStubsTask.libsDir).apply {
            include("**/*.bc")
        })

        interops.add(interop)
    }

    fun useInterop(interop: String) {
        useInterop(project.konanInteropContainer.getByName(interop))
    }

    fun useInterops(interops: Collection<Any>) {
        interops.forEach {
            when(it) {
                is String -> useInterop(it)
                is KonanInteropConfig -> useInterop(it)
                else -> throw IllegalArgumentException("Cannot convert the object to an interop description: $it")
            }
        }
    }

    // DSL. Input/output files

    fun inputDir(dir: String) = with(compilationTask) {
        _inputFiles.add(project.fileTree(dir))
    }
    fun inputFiles(vararg files: Any) = with(compilationTask) {
        _inputFiles.add(project.files(files))
    }
    fun inputFiles(files: FileCollection) = compilationTask._inputFiles.add(files)
    fun inputFiles(files: Collection<FileCollection>) = compilationTask._inputFiles.addAll(files)


    fun outputDir(dir: Any) = with(compilationTask) {
        outputDir = project.file(dir)
    }

    fun outputName(name: String) = with(compilationTask) {
        artifactName = name
    }

    // DSL. Libraries.

    fun library(lib: Any) = libraries(lib)
    fun libraries(vararg libs: Any) = with(compilationTask) {
        libraries.add(project.files(*libs))
    }
    fun libraries(libs: FileCollection) = with(compilationTask) {
        libraries.add(libs)
    }

    fun nativeLibrary(lib: Any) = nativeLibraries(lib)
    fun nativeLibraries(vararg libs: Any) = with(compilationTask) {
        nativeLibraries.add(project.files(*libs))
    }
    fun nativeLibraries(libs: FileCollection) = with(compilationTask) {
        nativeLibraries.add(libs)
    }

    // DSL. Other parameters.

    fun linkerOpts(args: List<String>) = linkerOpts(*args.toTypedArray())
    fun linkerOpts(vararg args: String) = with(compilationTask) {
        _linkerOpts.addAll(args)
    }

    fun manifest(arg: Any) = with(compilationTask) {
        manifest = project.file(arg)
    }

    fun target(tgt: String) = with(compilationTask) {
        target = tgt
    }

    fun languageVersion(version: String) = with(compilationTask) {
        languageVersion = version
    }

    fun apiVersion(version: String) = with(compilationTask) {
        apiVersion = version
    }

    fun enableDebug(flag: Boolean) = with(compilationTask) {
        enableDebug = flag
    }

    fun noStdLib() = with(compilationTask) {
        noStdLib = true
    }

    fun produce(prod: String) = with(compilationTask) {
        produce = prod
    }

    fun noMain() = with(compilationTask) {
        noMain = true
    }

    fun enableOptimization() = with(compilationTask) {
        enableOptimization = true
    }

    fun enableAssertions() = with(compilationTask) {
        enableAssertions = true
    }

    fun dumpParameters(value: Boolean) = with(compilationTask) {
        dumpParameters = value
    }

    fun dependsOn(dependency: Any) = compilationTask.dependsOn(dependency)
}
