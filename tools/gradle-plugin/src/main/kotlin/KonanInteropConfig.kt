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
import org.gradle.api.internal.project.ProjectInternal

/**
 *  What we can:
 *
 *  konanInterop {
 *      foo {
 *          defFile <def-file>
 *          pkg <package with stubs>
 *          target <target: linux/macbook/iphone/iphone_sim>
 *          compilerOpts <Options for native stubs compilation>
 *          linkerOpts <Options for native stubs >
 *          headers <headers to process>
 *          includeDirs <directories where headers are located>
 *          linkFiles <files which will be linked with native stubs>
 *      }
 *
 *      // TODO: add configuration for konan compiler
 *  }
 */

open class KonanInteropConfig(
        val configName: String,
        val project: ProjectInternal
): Named {

    override fun getName() = configName

    // Child tasks ------------------------------------------------------------

    // Task to process the library and generate stubs
    val generateStubsTask: KonanInteropTask = project.tasks.create(
            "gen${name.capitalize()}InteropStubs",
            KonanInteropTask::class.java
    )

    // Config and task to compile *.kt stubs in a *.bc library
    internal val compileStubsConfig = KonanCompilerConfig("${name}InteropStubs", project, "compile").apply {
        compilationTask.dependsOn(generateStubsTask)
        outputDir(generateStubsTask.stubsDir.absolutePath)
        noLink()
        inputFiles(project.fileTree(generateStubsTask.stubsDir).apply { builtBy(generateStubsTask) })
    }
    val compileStubsTask = compileStubsConfig.compilationTask

    // DSL methods ------------------------------------------------------------

    fun defFile(file: Any) = with(generateStubsTask) {
        defFile = project.file(file)
    }

    fun pkg(value: String) = with(generateStubsTask) {
        pkg = value
    }

    fun target(value: String) = with(generateStubsTask) {
        generateStubsTask.target = value
        compileStubsTask.target = value
    }

    fun compilerOpts(vararg values: String) = with(generateStubsTask) {
        compilerOpts.addAll(values)
    }

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any) = with(generateStubsTask) {
        headers.add(project.files(files))
    }
    fun headers(files: FileCollection) = with(generateStubsTask) {
        headers.add(files)
    }


    fun includeDirs(vararg values: String) = with(generateStubsTask) {
        compilerOpts.addAll(values.map { "-I$it" })
    }

    fun linker(value: String) = with(generateStubsTask) {
        linker = value
    }

    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    fun linkerOpts(values: List<String>) = with(generateStubsTask) {
        linkerOpts.addAll(values)
    }

    fun link(vararg files: Any) = with(generateStubsTask) {
        linkFiles.add(project.files(files))
    }
    fun link(files: FileCollection) = with(generateStubsTask) {
        linkFiles.add(files)
    }

}
