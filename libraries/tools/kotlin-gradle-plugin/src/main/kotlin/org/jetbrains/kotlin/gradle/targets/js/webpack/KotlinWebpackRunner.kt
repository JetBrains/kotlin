/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.jetbrains.kotlin.gradle.internal.TeamCityMessageCommonClient
import org.jetbrains.kotlin.gradle.internal.execWithErrorLogger
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import java.io.File

internal data class KotlinWebpackRunner(
    val npmProject: NpmProject,
    val configFile: File,
    val execHandleFactory: ExecHandleFactory,
    val tool: String,
    val args: List<String>,
    val nodeArgs: List<String>,
    val config: KotlinWebpackConfig
) {
    fun execute() = npmProject.project.execWithErrorLogger("webpack") { execAction, progressLogger ->
        val client = configureClient(progressLogger)
        client.apply {
            configureExec(
                execAction,
                client
            )
        }
    }

    fun start(): ExecHandle {
        val execFactory = execHandleFactory.newExec()
        configureExec(
            execFactory,
            configureClient(null)
        )
        val exec = execFactory.build()
        exec.start()
        return exec
    }

    private fun configureClient(progressLogger: ProgressLogger?): TeamCityMessageCommonClient {
        val logger = npmProject.project.logger
        return TeamCityMessageCommonClient(logger)
            .apply {
                if (progressLogger != null) {
                    this.progressLogger = progressLogger
                }
            }
    }

    private fun configureExec(
        execFactory: ExecSpec,
        client: TeamCityMessageCommonClient
    ) {
        check(config.entry?.isFile == true) {
            "${this}: Entry file not existed \"${config.entry}\""
        }

        execFactory.standardOutput = TCServiceMessageOutputStreamHandler(
            client = client,
            onException = { },
            logger = client.log
        )
        execFactory.errorOutput = TCServiceMessageOutputStreamHandler(
            client = client,
            onException = { },
            logger = client.log
        )

        config.save(configFile)

        val args = mutableListOf<String>("--config", configFile.absolutePath)
        if (config.showProgress) {
            args.add("--progress")
        }

        args.addAll(this.args)

        npmProject.useTool(
            execFactory,
            tool,
            nodeArgs,
            args
        )
    }
}