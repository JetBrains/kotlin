/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import com.google.gson.annotations.Expose
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.io.FileReader
import java.io.FileWriter

internal data class Entry(
        @Expose val directory: String,
        @Expose val file: String,
        @Expose val arguments: List<String>,
        @Expose val output: String
) {
    companion object {
        fun create(
                directory: File,
                file: File,
                args: List<String>,
                outputDir: File
        ): Entry {
            return Entry(
                    directory.absolutePath,
                    file.absolutePath,
                    args + listOf(file.absolutePath),
                    File(outputDir, file.name + ".o").absolutePath
            )
        }

        fun writeListTo(file: File, entries: List<Entry>) = FileWriter(file).use {
            gson.toJson(entries, it)
        }

        fun readListFrom(file: File): Array<Entry> = FileReader(file).use {
            gson.fromJson(it, Array<Entry>::class.java)
        }
    }
}

open class GenerateCompilationDatabase @Inject constructor(@Input val target: String,
                                                           @Internal val files: Iterable<File>,
                                                           @Input val executable: String,
                                                           @Input val compilerFlags: List<String>,
                                                           @Internal val outputDir: File
) : DefaultTask() {
    @OutputFile
    var outputFile = File(outputDir, "compile_commands.json")

    // Annotate as an input because this path affects the content of the generated file.
    @get:Input
    val outputDirPath: String
        get() = outputDir.absolutePath

    @get:Input
    val pathsToFiles: Iterable<String>
        get() = files.map { it.absolutePath }

    @TaskAction
    fun run() {
        val plugin = project.extensions.getByType<ExecClang>()
        val executable = plugin.resolveExecutable(executable)
        val args = listOf(executable) + compilerFlags + plugin.clangArgsForCppRuntime(target)
        val entries: List<Entry> = files.map { Entry.create(it.parentFile, it, args, outputDir) }
        Entry.writeListTo(outputFile, entries)
    }
}

open class MergeCompilationDatabases @Inject constructor() : DefaultTask() {
    @InputFiles
    val inputFiles = mutableListOf<File>()

    @OutputFile
    var outputFile = File(project.buildDir, "compile_commands.json")

    @TaskAction
    fun run() {
        val entries = mutableListOf<Entry>()
        for (file in inputFiles) {
            entries.addAll(Entry.readListFrom(file))
        }
        Entry.writeListTo(outputFile, entries)
    }
}

fun mergeCompilationDatabases(project: Project, name: String, paths: List<String>): Task {
    val subtasks: List<MergeCompilationDatabases> = paths.map {
        val task = project.tasks.getByPath(it)
        if (task !is MergeCompilationDatabases) {
            throw GradleException("Unknown task type for compdb merging: $task")
        }
        task
    }
    return project.tasks.create(name, MergeCompilationDatabases::class.java) {
        dependsOn(subtasks)
        inputFiles.addAll(subtasks.map { it.outputFile })
    }
}
