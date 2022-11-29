/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.SanitizerKind
import org.jetbrains.kotlin.konan.target.TargetDomainObjectContainer
import org.jetbrains.kotlin.konan.target.targetSuffix
import org.jetbrains.kotlin.utils.Maybe
import org.jetbrains.kotlin.utils.asMaybe
import javax.inject.Inject

/**
 * Generating [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html).
 *
 * Allows generating compilation databases for different targets. For example:
 * ```
 * compilationDatabase {
 *    target(someTarget1) {
 *        entry { ... }
 *    }
 *    allTargets {
 *        mergeFrom(provider { project("subproject") })
 *    }
 * }
 * ```
 * Adds an entry for target `someTarget1`, and for each known target merges in databases generated
 * by this plugin in `subproject`.
 * The task that generates the database can be found via `compilationDatabase.target(someTarget1).task`.
 *
 * @see CompilationDatabasePlugin gradle plugin that creates this extension.
 */
abstract class CompilationDatabaseExtension @Inject constructor(private val project: Project) : TargetDomainObjectContainer<CompilationDatabaseExtension.Target>(project) {
    init {
        this.factory = { target, sanitizer ->
            project.objects.newInstance<Target>(project, target, sanitizer.asMaybe)
        }
    }

    /**
     * Entries in the compilation database.
     *
     * Single [Entry] generates a number of compilation database entries: one for each file in [files].
     *
     * @property target target for which this [Entry] is generated.
     * @property sanitizer optional sanitizer for [target].
     */
    abstract class Entry @Inject constructor(
            val target: KonanTarget,
            _sanitizer: Maybe<SanitizerKind>,
    ) {
        val sanitizer = _sanitizer.orNull

        /**
         * **directory** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * The working directory of the compilation. All paths in the [arguments] must either be absolute or relative to this directory.
         */
        abstract val directory: DirectoryProperty

        /**
         * **file** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * Collection of files being compiled with given [arguments]. For each file a separate
         * entry will be generated in the database with the same [directory], [arguments] and [output].
         */
        abstract val files: ConfigurableFileCollection

        /**
         * **arguments** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * List of arguments of the compilation commands. The first argument must be the executable.
         */
        abstract val arguments: ListProperty<String>

        /**
         * **output** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
         *
         * The name of the output created by the compilation step. Used as a key to distinguish
         * between different modes of compilation of the same sources.
         */
        abstract val output: Property<String>
    }

    /**
     * Configure compilation database generation for [target].
     *
     * [entry] to add new entries.
     * [mergeFrom] to merge databases from other projects with [CompilationDatabasePlugin]s.
     * [task] is the gradle task for compilation database generation.
     *
     * @property target target for which compilation database is generated.
     * @property sanitizer optional sanitizer for which compilation database is generated.
     */
    abstract class Target @Inject constructor(
            private val project: Project,
            val target: KonanTarget,
            _sanitizer: Maybe<SanitizerKind>,
    ) {
        val sanitizer = _sanitizer.orNull

        protected abstract val mergeFrom: ListProperty<GenerateCompilationDatabase>

        /**
         * Merge compilation database generated for [from] project for [target] with optional [sanitizer].
         *
         * @param from project with applied [CompilationDatabasePlugin] to merge compilation database from.
         */
        fun mergeFrom(from: Provider<Project>) {
            mergeFrom.add(from.flatMap { project ->
                project.extensions.getByType<CompilationDatabaseExtension>().target(target, sanitizer).task
            })
        }

        protected abstract val entries: ListProperty<GenerateCompilationDatabase.Entry>

        /**
         * Add an entry to the compilation database for [target] with optional [sanitizer].
         *
         * @param action configure [Entry]
         */
        fun entry(action: Action<in Entry>) {
            entries.add(project.provider {
                val instance = project.objects.newInstance<Entry>(target, sanitizer.asMaybe).apply {
                    action.execute(this)
                }
                project.objects.newInstance<GenerateCompilationDatabase.Entry>().apply {
                    directory.set(instance.directory)
                    files.from(instance.files)
                    arguments.set(instance.arguments)
                    output.set(instance.output)
                }
            })
        }

        /**
         * Gradle task that generates compilation database for [target] with optional [sanitizer].
         */
        val task = project.tasks.register<GenerateCompilationDatabase>("${target}${sanitizer.targetSuffix}CompilationDatabase") {
            description = "Generate compilation database for $target${sanitizer.targetSuffix}"
            group = TASK_GROUP
            mergeFiles.from(mergeFrom)
            entries.set(this@Target.entries)
            outputFile.set(project.layout.buildDirectory.file("${target}${sanitizer.targetSuffix}/compile_commands.json"))
        }
    }

    companion object {
        @JvmStatic
        val TASK_GROUP = "development support"
    }
}

/**
 * Generating [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html).
 *
 * Creates [CompilationDatabaseExtension] extension named `compilationDatabase`.
 *
 * @see CompilationDatabaseExtension extension that this plugin creates.
 */
open class CompilationDatabasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create<CompilationDatabaseExtension>("compilationDatabase", project)
    }
}
