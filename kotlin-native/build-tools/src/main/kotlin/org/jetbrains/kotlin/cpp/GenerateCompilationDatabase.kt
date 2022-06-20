/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import com.google.gson.annotations.Expose
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gson
import java.io.FileReader
import java.io.FileWriter

/**
 * Generating [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html).
 *
 * Generate compilation database in [outputFile] from [entries] and merging in already
 * generated databases in [mergeFiles].
 *
 * @see CompilationDatabaseExtension gradle plugin to simplify generation of these tasks.
 */
abstract class GenerateCompilationDatabase : DefaultTask() {
    /**
     * Entries in the compilation database.
     *
     * Single [Entry] generates a number of compilation database entries: one for each file in [files].
     */
    abstract class Entry {
        /**
         * **directory** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * The working directory of the compilation. All paths in the [arguments] must either be absolute or relative to this directory.
         */
        @get:Internal
        abstract val directory: DirectoryProperty

        // Only the path of the directory is an input.
        @get:Input
        protected val pathToDirectory: Provider<String>
            get() = directory.asFile.map { it.absolutePath }

        /**
         * **file** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * Collection of files being compiled with given [arguments]. For each file a separate
         * entry will be generated in the database with the same [directory], [arguments] and [output].
         */
        @get:Internal
        abstract val files: ConfigurableFileCollection

        // Only the paths of files are an input.
        @get:Input
        protected val pathsToFiles: Iterable<String>
            get() = files.files.map { it.absolutePath }

        /**
         * **arguments** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * List of arguments of the compilation commands. The first argument must be the executable.
         */
        @get:Input
        abstract val arguments: ListProperty<String>

        /**
         * **output** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * The name of the output created by the compilation step. Used as a key to distinguish
         * between different modes of compilation of the same sources.
         */
        @get:Input
        abstract val output: Property<String>
    }

    // Must follow https://clang.llvm.org/docs/JSONCompilationDatabase.html#format
    private data class SerializedEntry(
            @Expose val directory: String,
            @Expose val file: String,
            @Expose val arguments: List<String>,
            @Expose val output: String,
    ) {
        companion object {
            fun fromEntry(entry: Entry): List<SerializedEntry> {
                val directory = entry.directory.asFile.get().absolutePath
                val arguments = entry.arguments.get()
                val output = entry.output.get()
                return entry.files.map {
                    val file = it.absolutePath
                    SerializedEntry(
                            directory,
                            file,
                            arguments + listOf(file),
                            output,
                    )
                }
            }
        }
    }

    /**
     * List of [Entry]s to generate database from.
     */
    @get:Nested
    abstract val entries: ListProperty<Entry>

    /**
     * List of databases to merge into this database.
     */
    @get:InputFiles
    abstract val mergeFiles: ConfigurableFileCollection

    /**
     * Where to put the database.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val serialized = mutableListOf<SerializedEntry>()
        mergeFiles.files.forEach { file ->
            FileReader(file).use {
                serialized.addAll(gson.fromJson(it, Array<SerializedEntry>::class.java))
            }
        }
        entries.get().forEach {
            // TODO: Reconsider when we use source directory for this.
            // Make sure directories actually exist.
            it.directory.asFile.get().mkdirs()
            serialized.addAll(SerializedEntry.fromEntry(it))
        }
        FileWriter(outputFile.asFile.get()).use {
            gson.toJson(serialized, it)
        }
    }
}
