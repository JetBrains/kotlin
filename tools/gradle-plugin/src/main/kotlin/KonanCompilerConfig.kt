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
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
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
 *          noLink
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
// TODO: check debug outputs
// TODO: Use +=/-= syntax for libraries and inputFiles
open class KonanCompilerConfig(
        val configName: String,
        val project: ProjectInternal,
        taskNamePrefix: String = "compileKonan"): Named {

    override fun getName() = configName

    val compilationTask: KonanCompileTask = project.tasks.create(
            "$taskNamePrefix${configName.capitalize()}",
            KonanCompileTask::class.java
    ) { it.initialize(this@KonanCompilerConfig.name) }

    protected fun String.toAbsolutePath() = project.file(this).canonicalPath

    // DSL methods --------------------------------------------------

    // TODO: Check if we copied all data or not
    fun extendsFrom(anotherConfig: KonanCompilerConfig) = with(compilationTask) {
        val anotherTask = anotherConfig.compilationTask

        outputDir(anotherTask.outputDir.absolutePath)

        linkerOpts(anotherTask.linkerOpts)

        anotherTask.target?.let { target(it) }
        anotherTask.languageVersion?.let { languageVersion(it) }
        anotherTask.apiVersion?.let { apiVersion(it) }

        if (anotherTask.noStdLib) noStdLib()
        if (anotherTask.noLink) noLink()
        if (anotherTask.noMain) noMain()
        if (anotherTask.enableOptimization) enableOptimization()
        if (anotherTask.enableAssertions) enableAssertions()
    }

    fun useInterop(interopConfig: KonanInteropConfig) {
        val generateStubsTask = interopConfig.generateStubsTask
        val compileStubsTask  = interopConfig.compileStubsTask

        compilationTask.dependsOn(compileStubsTask)
        compilationTask.dependsOn(generateStubsTask)

        linkerOpts(generateStubsTask.linkerOpts)
        libraries(project.fileTree(compileStubsTask.outputDir).apply {
            builtBy(compileStubsTask)
            include("**/*.bc")
        })
        nativeLibraries(project.fileTree(generateStubsTask.libsDir).apply {
            builtBy(generateStubsTask)
            include("**/*.bc")
        })
    }
    fun useInterop(interop: String) = useInterop(project.konanInteropContainer.getByName(interop))

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

    fun target(tgt: String) = with(compilationTask) {
        target = tgt
    }

    fun languageVersion(version: String) = with(compilationTask) {
        languageVersion = version
    }

    fun apiVersion(version: String) = with(compilationTask) {
        apiVersion = version
    }

    fun noStdLib() = with(compilationTask) {
        noStdLib = true
    }

    fun noLink() = with(compilationTask) {
        noLink = true
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
}