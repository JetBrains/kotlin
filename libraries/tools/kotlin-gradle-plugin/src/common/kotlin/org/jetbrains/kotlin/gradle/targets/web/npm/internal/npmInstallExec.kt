/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.internal

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.internal.newBuildOpLogger
import java.io.File

/**
 * Runs `npm install` in [workingDir].
 *
 * Infrastructure-agnostic: used both by the legacy root-project based NPM resolution
 * and by the Isolated Projects compatible one.
 *
 * @param nodeExecutable the Node.js executable, used to run [npmExecutable] unless it is [standalone].
 * @param npmExecutable either a standalone `npm` command, or the `npm-cli.js` script of a Node.js distribution.
 * @param standalone whether [npmExecutable] is an executable on its own, rather than a script to run with Node.js.
 * @param ignoreScripts whether to pass `--ignore-scripts` to npm.
 * @param args additional npm arguments.
 */
internal fun npmInstallExec(
    objects: ObjectFactory,
    execOps: ExecOperations,
    logger: Logger,
    description: String,
    nodeExecutable: String,
    npmExecutable: String,
    standalone: Boolean,
    ignoreScripts: Boolean,
    workingDir: File,
    args: List<String>,
) {
    val progressLogger = objects.newBuildOpLogger()
    execWithProgress(progressLogger, description, execOps = execOps) { execSpec ->
        val arguments = buildList {
            add("install")
            addAll(args.filter(String::isNotEmpty))
            if (logger.isDebugEnabled) add("--verbose")
            if (ignoreScripts) add("--ignore-scripts")
        }

        if (!standalone) {
            val nodePath = File(nodeExecutable).parent
            execSpec.environment["PATH"] =
                "$nodePath${File.pathSeparator}${System.getenv("PATH")}"
        }

        if (standalone) {
            execSpec.executable = npmExecutable
            execSpec.setArgs(arguments)
        } else {
            execSpec.executable = nodeExecutable
            execSpec.setArgs(listOf(npmExecutable) + arguments)
        }

        execSpec.workingDir = workingDir
    }
}
