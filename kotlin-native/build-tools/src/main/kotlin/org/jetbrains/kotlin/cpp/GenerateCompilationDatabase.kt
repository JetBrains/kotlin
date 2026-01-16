/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.Expose
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
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
@DisableCachingByDefault(because = "No point in caching")
abstract class GenerateCompilationDatabase : DefaultTask() {
    // Must follow https://clang.llvm.org/docs/JSONCompilationDatabase.html#format
    private data class SerializedEntry(
            @Expose val directory: String,
            @Expose val file: String,
            @Expose val arguments: List<String>,
            @Expose val output: String,
    ) {
        companion object {
            fun fromEntry(entry: CompilationDatabaseEntry): List<SerializedEntry> {
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
     * List of [CompilationDatabaseEntry]s to generate database from.
     */
    @get:Nested
    abstract val entries: ListProperty<CompilationDatabaseEntry>

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
                try {
                    serialized.addAll(gson.fromJson(it, Array<SerializedEntry>::class.java))
                } catch (e: JsonSyntaxException) {
                    throw IllegalStateException("Failed to parse $file as compilation database", e)
                }
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
