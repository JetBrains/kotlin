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

import org.gradle.api.DefaultTask
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
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


// TODO: form groups for tasks
// TODO: Make the task class nested for config with properties accessible for outer users.
open class KonanCompileTask: KonanTargetableTask() {

    companion object {
        const val COMPILER_MAIN = "org.jetbrains.kotlin.cli.bc.K2NativeKt"
    }

    val COMPILER_JVM_ARGS: List<String>
        get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")
    val COMPILER_CLASSPATH: String
        get() = "${project.konanHome}/konan/lib/"

    // Output artifact --------------------------------------------------------

    internal lateinit var artifactName: String

    lateinit var outputDir: File
        internal set

    internal fun init(artifactName: String) {
        dependsOn(project.konanCompilerDownloadTask)
        this.artifactName = artifactName
        outputDir = project.file("${project.konanCompilerOutputDir}")
    }

    val artifactNamePath: String
        get() = "${outputDir.absolutePath}/$artifactName"

    val artifactSuffix: String
        get() = produceSuffix(produce)

    val artifactPath: String
        get() = "$artifactNamePath$artifactSuffix"

    val artifact: File
        @OutputFile get() = project.file(artifactPath)

    // Other compilation parameters -------------------------------------------

    @InputFiles val inputFiles      = mutableSetOf<FileCollection>()

    @InputFiles val libraries       = mutableSetOf<FileCollection>()
    @InputFiles val nativeLibraries = mutableSetOf<FileCollection>()

    @Input var produce              = "program"
        internal set

    @Input var linkerOpts = mutableListOf<String>()
        internal set

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
                classpath = project.fileTree(COMPILER_CLASSPATH).apply { include("*.jar") }
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
        val project: ProjectInternal,
        taskNamePrefix: String = "compileKonan"): Named {

    override fun getName() = configName

    val compilationTask: KonanCompileTask = project.tasks.create(
            "$taskNamePrefix${configName.capitalize()}",
            KonanCompileTask::class.java
    ) { it.init(this@KonanCompileConfig.name) }

    // DSL methods --------------------------------------------------

    // TODO: Check if we copied all data or not
    fun extendsFrom(anotherConfig: KonanCompileConfig) = with(compilationTask) {
        val anotherTask = anotherConfig.compilationTask

        outputDir(anotherTask.outputDir.absolutePath)

        linkerOpts(anotherTask.linkerOpts)

        anotherTask.target?.let { target(it) }
        anotherTask.languageVersion?.let { languageVersion(it) }
        anotherTask.apiVersion?.let { apiVersion(it) }
        anotherTask.produce.let { produce(it) }

        enableDebug(anotherTask.enableDebug)
        if (anotherTask.noStdLib) noStdLib()
        if (anotherTask.noMain) noMain()
        if (anotherTask.enableOptimization) enableOptimization()
        if (anotherTask.enableAssertions) enableAssertions()
    }

    private fun useInteropFromConfig(interopConfig: KonanInteropConfig) {
        val generateStubsTask = interopConfig.generateStubsTask
        val compileStubsTask  = interopConfig.compileStubsTask

        compilationTask.dependsOn(compileStubsTask)
        compilationTask.dependsOn(generateStubsTask)

        linkerOpts(generateStubsTask.linkerOpts)
        library(compileStubsTask.artifact)
        nativeLibraries(project.fileTree(generateStubsTask.libsDir).apply {
            builtBy(generateStubsTask)
            include("**/*.bc")
        })
        generateStubsTask.manifest ?.let {manifest(it)}
    }

    fun useInterops(interops: ArrayList<String>) {
        interops.forEach { useInteropFromConfig(project.konanInteropContainer.getByName(it)) }
    }

    fun useInterop(interop: String) {
        useInteropFromConfig(project.konanInteropContainer.getByName(interop))
    }

    // DSL. Input/output files

    fun inputDir(dir: String) = with(compilationTask) {
        inputFiles.add(project.fileTree(dir))
    }
    fun inputFiles(vararg files: Any) = with(compilationTask) {
        inputFiles.add(project.files(files))
    }
    fun inputFiles(files: FileCollection) = compilationTask.inputFiles.add(files)
    fun inputFiles(files: Collection<FileCollection>) = compilationTask.inputFiles.addAll(files)


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
        linkerOpts.addAll(args)
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
}
