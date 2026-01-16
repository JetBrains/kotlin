/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.konan.target.TargetDomainObjectContainer
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.utils.capitalized
import javax.inject.Inject

/**
 * Generating [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html).
 *
 * Allows generating compilation databases for different targets. For example:
 * ```
 * dependencies {
 *     compilationDatabase(project(":some:other:project"))
 * }
 *
 * compilationDatabase {
 *    target(someTarget1) {
 *        entry { ... }
 *    }
 *    allTargets {}
 * }
 * ```
 * Adds an entry for target `someTarget1`, and for each known target merges in databases from `:some:other:project`.
 * The task that generates the database can be found via `compilationDatabase.target(someTarget1).task`.
 *
 * @see CompilationDatabasePlugin gradle plugin that creates this extension.
 */
abstract class CompilationDatabaseExtension @Inject constructor(private val project: Project) : TargetDomainObjectContainer<CompilationDatabaseExtension.Target>(project) {
    init {
        this.factory = { target ->
            project.objects.newInstance<Target>(this, target)
        }
    }

    /**
     * Bucket of dependencies with compilation databases to merge in.
     */
    val compilationDatabase: Configuration by project.configurations.creating {
        description = "Compilation Database dependencies"
        isCanBeConsumed = false
        isCanBeResolved = false
    }

    /**
     * Internal resolvable configuration of compilation database dependencies.
     */
    private val compilationDatabaseJSON by project.configurations.creating {
        description = "Compilation Database dependencies (internal)"
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(CppUsage.USAGE_ATTRIBUTE, project.objects.named(CppUsage.COMPILATION_DATABASE))
        }
        extendsFrom(compilationDatabase)
    }

    /**
     * Contains produced compilation database.
     */
    val compilationDatabaseElements: Configuration by project.configurations.creating {
        description = "Compilation Database"
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(CppUsage.USAGE_ATTRIBUTE, project.objects.named(CppUsage.COMPILATION_DATABASE))
        }
    }

    /**
     * Configure compilation database generation for [target].
     *
     * [entry] to add new entries.
     * [task] is the gradle task for compilation database generation.
     *
     * @property target target for which compilation database is generated.
     * @property sanitizer optional sanitizer for which compilation database is generated.
     */
    abstract class Target @Inject constructor(
            private val owner: CompilationDatabaseExtension,
            private val _target: TargetWithSanitizer,
    ) {
        val target by _target::target
        val sanitizer by _target::sanitizer

        private val project by owner::project

        protected abstract val entries: ListProperty<CompilationDatabaseEntry>

        /**
         * Add an entry to the compilation database for [target] with optional [sanitizer].
         *
         * @param action configure [CompilationDatabaseEntry]
         */
        fun entry(action: Action<in CompilationDatabaseEntry>) {
            entries.add(project.provider {
                project.objects.newInstance<CompilationDatabaseEntry>(_target).apply {
                    action.execute(this)
                }
            })
        }

        /**
         * Gradle task that generates compilation database for [target] with optional [sanitizer].
         */
        val task = project.tasks.register<GenerateCompilationDatabase>("compilationDatabase${_target.name.capitalized}") {
            description = "Generate compilation database for $_target"
            group = TASK_GROUP
            mergeFiles.from(
                    owner.compilationDatabaseJSON.incoming.artifactView {
                        attributes {
                            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, _target)
                        }
                    }.files)
            entries.set(this@Target.entries)
            outputFile.set(project.layout.buildDirectory.file("$_target/compile_commands.json"))
        }

        init {
            owner.compilationDatabaseElements.outgoing {
                variants {
                    create("$_target") {
                        attributes {
                            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, _target)
                        }
                        artifact(task)
                    }
                }
            }
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
 * Creates the following [configurations][org.gradle.api.artifacts.Configuration]:
 * * `compilationDatabase` - like `implementation` from java or C++ plugin. Bucket of dependencies to get other compilation databases from.
 * * `compilationDatabaseElements` - like `apiElements` (sort of) from java plugin or `{variant}LinkElements` from C++ plugin. Contains produced compilation database.
 *
 * @see CompilationDatabaseExtension extension that this plugin creates.
 */
open class CompilationDatabasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<CppConsumerPlugin>()
        project.extensions.create<CompilationDatabaseExtension>("compilationDatabase", project)
    }
}
