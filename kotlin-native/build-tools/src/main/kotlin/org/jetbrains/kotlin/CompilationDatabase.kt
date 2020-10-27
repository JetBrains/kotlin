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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.bitcode.CompileToBitcode
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
                                                           @Input val srcRoot: File,
                                                           @Input val files: Iterable<File>,
                                                           @Input val executable: String,
                                                           @Input val compilerFlags: List<String>,
                                                           @Input val outputDir: File
) : DefaultTask() {
    @OutputFile
    var outputFile = File(outputDir, "compile_commands.json")

    @TaskAction
    fun run() {
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        val executable = plugin.resolveExecutable(executable)
        val args = listOf(executable) + compilerFlags + plugin.konanArgs(target)
        val entries: List<Entry> = files.map { Entry.create(srcRoot, it, args, outputDir) }
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
    return project.tasks.create(name, MergeCompilationDatabases::class.java) { task ->
        task.dependsOn(subtasks)
        task.inputFiles.addAll(subtasks.map { it.outputFile })
    }
}

/**
 * Get all [CompileToBitcode] tasks in the project, group them by target, and generate
 * compilation database for each target. Tasks will be named <target>[name]. Databases
 * will be placed in a build dir in <target>/compile_commands.json
 */
fun createCompilationDatabasesFromCompileToBitcodeTasks(project: Project, name: String) {
    val compileTasks = project.tasks.withType(CompileToBitcode::class.java).toList()
    val compdbTasks = compileTasks.groupBy({ task -> task.target }) { task ->
        project.tasks.create("${task.name}_CompilationDatabase",
                GenerateCompilationDatabase::class.java,
                task.target,
                task.srcRoot,
                task.inputFiles,
                task.executable,
                task.compilerFlags,
                task.objDir)
    }
    for ((target, tasks) in compdbTasks) {
        project.tasks.create("${target}${name}", MergeCompilationDatabases::class.java) { task ->
            task.dependsOn(tasks)
            task.inputFiles.addAll(tasks.map { it.outputFile })
            task.outputFile = File(File(project.buildDir, target), "compile_commands.json")
        }
    }
}
