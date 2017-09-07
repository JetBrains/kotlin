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
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.*
import java.io.File

/**
 * A task executing cinterop tool with the given args and compiling the stubs produced by this tool.
 */
open class KonanInteropTask: KonanTargetableTask() {

    internal fun init(libName: String) {
        dependsOn(project.konanCompilerDownloadTask)
        this.libName = libName
        this.defFile = project.konanDefaultDefFile(libName)
        this.kLib = outputDir.resolve("$libName.klib")
    }

    // Output directories -----------------------------------------------------

    @Internal val outputDir = project.file(project.konanInteropOutputDir)

    lateinit var kLib: File
        @OutputFile get

    // Interop stub generator parameters -------------------------------------

    @InputFile lateinit var defFile: File
        internal set

    @Optional @Input var pkg: String? = null
        internal set

    @Input lateinit var libName: String
        internal set

    @Input var dumpParameters = false
        internal set

    @Input val compilerOpts   = mutableListOf<String>()
    @Input val linkerOpts     = mutableListOf<String>()
    @Input val extraOpts      = mutableListOf<String>()

    // TODO: Check if we can use only one FileCollection instead of set.
    @InputFiles val headers   = mutableSetOf<FileCollection>()
    @InputFiles val linkFiles = mutableSetOf<FileCollection>()

    @Input val konanVersion = project.konanVersion

    @TaskAction
    fun exec() {
        outputDir.mkdirs()
        if (dumpParameters) dumpProperties(this@KonanInteropTask)
        KonanInteropRunner(project).run(buildArgs())
    }

    protected fun buildArgs() = mutableListOf<String>().apply {
        addArg("-properties", "${project.konanHome}/konan/konan.properties")

        addArg("-o", kLib.canonicalPath)

        addArgIfNotNull("-target", target)
        addArgIfNotNull("-def", defFile.canonicalPath)
        addArg("-pkg", pkg ?: libName)

        addFileArgs("-h", headers)

        compilerOpts.forEach {
            addArg("-copt", it)
        }

        val linkerOpts = mutableListOf<String>().apply { addAll(linkerOpts) }
        linkFiles.forEach {
            linkerOpts.addAll(it.files.map { it.canonicalPath })
        }
        linkerOpts.forEach {
            addArg("-lopt", it)
        }

        addAll(extraOpts)
    }

}

open class KonanInteropConfig(
        val configName: String,
        val project: Project
): Named {

    override fun getName() = configName

    // Child tasks ------------------------------------------------------------

    // Task to process the library and generate stubs
    val interopProcessingTask: KonanInteropTask = project.tasks.create(
            "process${name.capitalize()}Interop",
            KonanInteropTask::class.java) {
        it.init(name)
        it.group = BasePlugin.BUILD_GROUP
        it.description = "Generates stubs for the Kotlin/Native interop '$name'"
    }

    // DSL methods ------------------------------------------------------------

    fun defFile(file: Any) = with(interopProcessingTask) {
        defFile = project.file(file)
    }

    fun pkg(value: String) = with(interopProcessingTask) {
        pkg = value
    }

    fun target(value: String) = with(interopProcessingTask) {
        interopProcessingTask.target = value
    }

    fun compilerOpts(vararg values: String) = with(interopProcessingTask) {
        compilerOpts.addAll(values)
    }

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any) = with(interopProcessingTask) {
        headers.add(project.files(files))
    }
    fun headers(files: FileCollection) = with(interopProcessingTask) {
        headers.add(files)
    }


    fun includeDirs(vararg values: String) = with(interopProcessingTask) {
        compilerOpts.addAll(values.map { "-I$it" })
    }

    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    fun linkerOpts(values: List<String>) = with(interopProcessingTask) {
        linkerOpts.addAll(values)
    }

    fun link(vararg files: Any) = with(interopProcessingTask) {
        linkFiles.add(project.files(files))
    }
    fun link(files: FileCollection) = with(interopProcessingTask) {
        linkFiles.add(files)
    }

    fun dumpParameters(value: Boolean) = with(interopProcessingTask) {
        dumpParameters = value
    }

    fun dependsOn(dependency: Any) = interopProcessingTask.dependsOn(dependency)

    fun extraOpts(vararg values: Any) = extraOpts(values.asList())
    fun extraOpts(values: List<Any>) {
        values.mapTo(interopProcessingTask.extraOpts) { it.toString() }
    }
}

