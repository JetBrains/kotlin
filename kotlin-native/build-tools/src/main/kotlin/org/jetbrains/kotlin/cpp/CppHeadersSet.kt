/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

/**
 * A set of header search paths.
 *
 * Use [from] to add to the search paths, these will be passed relative to [workingDir].
 * Use [systemFrom] to add to the search paths as absolute paths, but these will also be
 * omitted from dependency tracking.
 *
 * @see cppHeadersSet
 */
open class CppHeadersSet @Inject constructor(
        objectFactory: ObjectFactory,
) {
    protected open class SingleCppHeadersSet @Inject constructor(
            objectFactory: ObjectFactory,
    ) {
        @get:Internal("only header files inside the directory need to be tracked")
        val root: DirectoryProperty = objectFactory.directoryProperty()

        @get:Internal("used to compute relative paths to root")
        val workingDir: DirectoryProperty = objectFactory.directoryProperty()

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @Suppress("unused") // used only by Gradle machinery via reflection.
        protected val headers = objectFactory.fileTree().apply {
            from(root)
            include("**/*.h", "**/*.hpp")
        }

        @get:Input
        @get:Optional
        protected val rootRelativeToWorkingDir: Provider<String> = root.map { root ->
            workingDir.orNull?.let { workingDir ->
                root.asFile.toRelativeString(workingDir.asFile)
            } as String
        }

        @get:Internal("Fully handled by others")
        val asCompilerArgument: Provider<String> = rootRelativeToWorkingDir.orElse(root.map { it.asFile.absolutePath }).map { "-I$it" }
    }

    private fun ObjectFactory.systemSingleCppHeadersSet(root: FileSystemLocation) = newInstance<SingleCppHeadersSet>().apply {
        this.root.set(root.asFile)
    }

    private fun ObjectFactory.userSingleCppHeadersSet(root: FileSystemLocation) = newInstance<SingleCppHeadersSet>().apply {
        this.root.set(root.asFile)
        this.workingDir.set(this@CppHeadersSet.workingDir)
    }

    /**
     * Header dirs for which relative path to [workingDir] will be taken into account.
     */
    @get:Internal("used to compute sets")
    protected val userHeaderDirs: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Header dirs for which their paths will be completely ignored.
     */
    @get:Internal("used to compute sets")
    protected val systemHeaderDirs: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Nested
    protected val sets: Provider<List<SingleCppHeadersSet>> = systemHeaderDirs.elements.zip(userHeaderDirs.elements) { system, user ->
        system.map { objectFactory.systemSingleCppHeadersSet(it) } + user.map { objectFactory.userSingleCppHeadersSet(it) }
    }

    /**
     * Working directory for the compiler.
     * Header directories added via [from] will have their paths saved relative to this working directory.
     */
    @get:Internal("used to construct SingleCppHeadersSet")
    val workingDir: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Add [paths] to the header search path.
     *
     * Will be passed to the compiler relative to [workingDir].
     */
    fun from(vararg paths: Any) {
        userHeaderDirs.from(*paths)
    }

    /**
     * Add [paths] to the header search path.
     *
     * Will be passed to the compiler as absolute paths.
     *
     * *NOTE*: paths to the directories are not tracked; can only be used for headers dependencies to which are tracked elsewhere
     * (e.g. headers inside llvm, or toolchains)
     */
    fun systemFrom(vararg paths: Any) {
        systemHeaderDirs.from(*paths)
    }

    /**
     * Get the header search path as compiler arguments.
     */
    @get:Internal("Fully handled by sets")
    val asCompilerArguments: Provider<List<String>> = sets.map {
        it.map { it.asCompilerArgument.get() }
    }
}

fun ObjectFactory.cppHeadersSet() = newInstance<CppHeadersSet>()