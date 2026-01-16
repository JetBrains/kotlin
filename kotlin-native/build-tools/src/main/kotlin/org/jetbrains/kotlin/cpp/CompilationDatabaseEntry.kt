/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import javax.inject.Inject

/**
 * Entries in the compilation database.
 *
 * Single [CompilationDatabaseEntry] generates a number of compilation database entries: one for each file in [files].
 */
open class CompilationDatabaseEntry @Inject constructor(
        objectFactory: ObjectFactory,
        private val _target: TargetWithSanitizer,
) {
    /**
     * Target for which this [CompilationDatabaseEntry] is generated.
     */
    @get:Internal("not used directly for any task result")
    val target by _target::target

    /**
     * Optional sanitizer for [target].
     */
    @get:Internal("not used directly for any task result")
    val sanitizer by _target::sanitizer

    /**
     * **directory** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
     *
     * The working directory of the compilation. All paths in the [arguments] must either be absolute or relative to this directory.
     */
    @get:Internal("Only the path to the directory is an input")
    val directory: DirectoryProperty = objectFactory.directoryProperty()

    // Only the path of the directory is an input.
    @get:Input
    @Suppress("unused") // used only by Gradle machinery via reflection
    protected val pathToDirectory: Provider<String>
        get() = directory.asFile.map { it.absolutePath }

    /**
     * **file** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
     *
     * Collection of files being compiled with given [arguments]. For each file a separate
     * entry will be generated in the database with the same [directory], [arguments] and [output].
     */
    @get:Internal("Only file paths are an input")
    val files: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    @Suppress("unused") // used only by Gradle machinery via reflection
    protected val pathsToFiles: Iterable<String>
        get() = files.files.map { it.absolutePath }

    /**
     * **arguments** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
     *
     * List of arguments of the compilation commands. The first argument must be the executable.
     */
    @get:Input
    val arguments: ListProperty<String> = objectFactory.listProperty(String::class)

    /**
     * Headers to add to the [arguments].
     */
    @get:Nested
    val headersDirs: CppHeadersSet = objectFactory.cppHeadersSetIgnoringHeadersContent() // compilation database does not depend on the content of headers

    /**
     * **output** from the [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html#format).
     *
     * The name of the output created by the compilation step. Used as a key to distinguish
     * between different modes of compilation of the same sources.
     */
    @get:Input
    val output: Property<String> = objectFactory.property(String::class)
}