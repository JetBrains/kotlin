/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.resolveLlvmUtility
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * A task that formats changed code with clang-format.
 *
 * Will run <git-clang-format> --binary <clang-format> --force $(git merge-base [parent] HEAD) [directory]
 */
@UntrackedTask(because = "Formatter should always run when asked")
abstract class GitClangFormat : DefaultTask() {
    private val platformManager = project.extensions.getByType<PlatformManager>()

    /**
     * Parent branch for the current branch.
     */
    @get:Option(option = "parent", description = "base parent of the branch; by default origin/master")
    @get:Input
    abstract val parent: Property<String>

    /**
     * Directory in which to format sources.
     */
    @get:Option(option = "directory", description = "directory in which to format sources; by default project's source directory")
    @get:Input
    abstract val directory: Property<String>

    /**
     * Whether to run the tool in interactive mode.
     */
    @get:Option(option = "interactive", description = "interactively ask about each change before applying; by default false")
    @get:Input
    abstract val interactive: Property<Boolean>

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @TaskAction
    fun run() {
        val gitClangFormat = platformManager.resolveLlvmUtility("git-clang-format")
        val clangFormat = platformManager.resolveLlvmUtility("clang-format")
        val parent = this.parent.get()
        val directory = this.directory.get()
        val interactive = this.interactive.get()

        val commit = ByteArrayOutputStream().let {
            execOperations.exec {
                executable = "git"
                args("merge-base", parent, "HEAD")
                standardOutput = it
            }
            it.toString().trim()
        }
        execOperations.exec {
            executable = gitClangFormat
            args("--binary", clangFormat)
            args("--force")
            if (interactive) {
                args("--patch")
                // Not a nice experience, but it works.
                standardInput = System.`in`
            }
            args(commit)
            args("--")
            args(directory)
        }
    }
}