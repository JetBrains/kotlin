/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.isolated

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.web.npm.internal.npmInstallExec
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

/**
 * Installs the npm dependencies of the shared npm root project, which is assembled by
 * [org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSetupSharedNpmProjectTask].
 *
 * Registered by the project that applies
 * [org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin],
 * and depended on by every task of the build that uses [KotlinAggregatedNpmWorkspaceService].
 *
 * The task is never up-to-date, because it also tells [KotlinAggregatedNpmWorkspaceService] where
 * the npm dependencies are installed, which must happen in every build.
 */
@DisableCachingByDefault(because = "Installs npm dependencies into a non-reproducible directory")
internal abstract class KotlinIsolatedNpmInstallTask : DefaultTask(), UsesKotlinAggregatedNpmWorkspaceService {

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val objects: ObjectFactory

    /**
     * The assembled shared npm root project, containing the `package.json` files of all the compilations of the build.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val sharedNpmProjectDirectory: DirectoryProperty

    /**
     * The Node.js executable used to run npm.
     */
    @get:Input
    abstract val nodeExecutable: Property<String>

    /**
     * Whether to run npm with `--ignore-scripts`.
     */
    @get:Input
    abstract val ignoreScripts: Property<Boolean>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun install() {
        val sharedNpmProjectDirectory = sharedNpmProjectDirectory.getFile()

        val nodeExecutable = nodeExecutable.get()
        val npmCliScript = File(nodeExecutable).parentFile?.resolve(NPM_CLI_SCRIPT)
        val standalone = npmCliScript == null || !npmCliScript.isFile

        npmInstallExec(
            objects = objects,
            execOps = execOps,
            logger = logger,
            description = "Installing npm dependencies",
            nodeExecutable = nodeExecutable,
            npmExecutable = if (standalone) NPM_COMMAND else npmCliScript!!.absolutePath,
            standalone = standalone,
            ignoreScripts = ignoreScripts.get(),
            workingDir = sharedNpmProjectDirectory,
            args = emptyList(),
        )

        aggregatedNpmWorkspaceService.get().npmInstalled(sharedNpmProjectDirectory)
    }

    companion object {
        const val NAME: String = "isolatedNpmInstall"

        /**
         * The path of the single installation task of the build.
         *
         * [org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin] may only be applied
         * to the root project, so the path is known to every project without accessing it.
         */
        const val TASK_PATH: String = ":$NAME"

        private const val NPM_CLI_SCRIPT = "npm-cli.js"
        private const val NPM_COMMAND = "npm"
    }
}
