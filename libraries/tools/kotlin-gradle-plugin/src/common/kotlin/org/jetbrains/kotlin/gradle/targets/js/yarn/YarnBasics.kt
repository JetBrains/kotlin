/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.internal.newBuildOpLogger
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution
import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel
import java.io.File

@ObsoleteNpmDependencyModel
abstract class YarnBasics internal constructor(
    private val execOps: ExecOperations,
    private val objects: ObjectFactory,
    /**
     * File used by Yarn as an inter-process mutex (`--mutex file:<path>`).
     *
     * The network mutex (`--mutex network`) is not used, because Yarn waiting for it may silently exit
     * without doing anything on Node.js 18+ (KT-82693).
     * See https://github.com/yarnpkg/yarn/issues/8960.
     */
    private val mutexFile: File,
) : NpmApiExecution<YarnEnvironment> {

    override fun packageManagerExec(
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        environment: YarnEnvironment,
        dir: Provider<File>,
        description: String,
        args: List<String>,
    ) {
        val progressLogger = objects.newBuildOpLogger()
        execWithProgress(progressLogger, description, execOps) { exec ->
            val arguments = args
                .plus(
                    if (logger.isDebugEnabled) "--verbose" else ""
                )
                .plus(
                    if (environment.ignoreScripts) "--ignore-scripts" else ""
                )
                .plus(
                    // It is necessary for different Yarn processes to not interfere with each other
                    listOf(
                        "--network-concurrency",
                        "1",
                        "--mutex",
                        "file:${mutexFile.absolutePath}",
                    )
                )
                .filter { it.isNotEmpty() }

            mutexFile.parentFile.mkdirs()

            val nodeExecutable = nodeJs.nodeExecutable
            if (!environment.ignoreScripts) {
                val nodePath = File(nodeExecutable).parent
                exec.environment("PATH", "$nodePath${File.pathSeparator}${System.getenv("PATH")}")
            }

            val command = environment.executable
            if (environment.standalone) {
                exec.executable = command
                exec.setArgs(arguments)
            } else {
                exec.executable = nodeExecutable
                exec.setArgs(listOf(command) + arguments)
            }

            exec.workingDir = dir.get()
        }
    }

    override fun prepareTooling(dir: File) {
        dir.resolve("yarn.lock")
            .outputStream()
            .use { out ->
                YarnBasics::class.java.getResourceAsStream("/org/jetbrains/kotlin/gradle/targets/js/yarn/yarn.lock")
                    ?.copyTo(out)
            }
    }
}
