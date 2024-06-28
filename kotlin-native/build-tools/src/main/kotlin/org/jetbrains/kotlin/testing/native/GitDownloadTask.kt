/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.testing.native

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.isNotEmpty
import java.net.URI
import java.util.*
import javax.inject.Inject

/**
 * Clones the given revision of the given Git repository to the given directory.
 */
@Suppress("UnstableApiUsage")
abstract class GitDownloadTask @Inject constructor(
        objects: ObjectFactory,
        private val execOperations: ExecOperations,
        private val fileOperations: FileOperations,
) : DefaultTask() {
    @get:Input
    abstract val repository: Property<URI>

    @get:Input
    abstract val revision: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @Option(option = "refresh",
            description = "Fetch and checkout the revision even if the output directory already contains it. " +
                    "All changes in the output directory will be overwritten")
    @get:Input
    val refresh: Property<Boolean> = objects.property<Boolean>().convention(false)

    init {
        /**
         * The download task should be executed in the following cases:
         *
         *  - The output directory doesn't exist or is empty;
         *  - Repository or revision was changed since the last execution;
         *  - A user forced rerunning this tasks manually (see [GitDownloadTask.refresh]).
         *
         * In all other cases we consider the task UP-TO-DATE.
         */
        outputs.upToDateWhen {
            val upToDate = !refresh.get() && outputDirectory.asFileTree.isNotEmpty
            if (upToDate) {
                logger.info("Skip cloning to avoid rewriting possible debug changes in ${outputDirectory.get().asFile.absolutePath}.")
            }
            upToDate
        }
    }

    private fun git(
            vararg args: String,
            ignoreExitValue: Boolean = false,
            execConfiguration: ExecSpec.() -> Unit = {}
    ): ExecResult =
            execOperations.exec {
                executable = "git"
                args(*args)
                isIgnoreExitValue = ignoreExitValue
                execConfiguration()
            }

    private fun tryCloneBranch(): Boolean {
        val execResult = git(
                "clone", repository.get().toString(),
                outputDirectory.get().asFile.absolutePath,
                "--depth", "1",
                "--branch", revision.get(),
                ignoreExitValue = true
        )
        return execResult.exitValue == 0
    }

    private fun fetchByHash() {
        git("init", outputDirectory.get().asFile.absolutePath)
        git("fetch", repository.get().toString(), "--depth", "1", revision.get()) {
            workingDir(outputDirectory)
        }
        git("reset", "--hard", revision.get()) {
            workingDir(outputDirectory)
        }
    }

    @TaskAction
    fun clone() {
        fileOperations.delete(outputDirectory)

        if (!tryCloneBranch()) {
            logger.info("Cannot use the revision '${revision.get()}' to clone the repository. Trying to use init && fetch instead.")
            fetchByHash()
        }

        // Delete the .git directory of the cloned repo to avoid adding it to IDEA's VCS roots.
        outputDirectory.dir(".git").get().asFile.deleteRecursively()
    }
}
