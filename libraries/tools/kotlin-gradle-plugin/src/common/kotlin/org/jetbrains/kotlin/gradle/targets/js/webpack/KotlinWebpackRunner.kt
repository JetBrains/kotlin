/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.internal.TeamCityMessageCommonClient
import org.jetbrains.kotlin.gradle.internal.execWithErrorLogger
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandle
import org.jetbrains.kotlin.gradle.utils.processes.ExecHandleBuilder
import org.jetbrains.kotlin.gradle.utils.processes.ExecResult
import org.jetbrains.kotlin.gradle.utils.processes.execHandleBuilder
import java.io.File

internal data class KotlinWebpackRunner(
    val npmProject: NpmProject,
    val logger: Logger,
    val configFile: File,
    val tool: String,
    val args: List<String>,
    val nodeArgs: List<String>,
    val config: KotlinWebpackConfig,
    private val objects: ObjectFactory,
) {
    fun execute(services: ServiceRegistry): ExecResult =
        services.execWithErrorLogger("webpack", objects = objects) { execAction, progressLogger ->
            configureExec(
                execAction,
                progressLogger
            )
        }

    fun start(): ExecHandle {
        val processRunnerBuilder = objects.execHandleBuilder {
            configureExec(this, null)
        }
        val processRunner = processRunnerBuilder.build()
        processRunner.execute()
        return processRunner
    }

    private fun configureClient(
        clientType: LogType,
        progressLogger: ProgressLogger?,
        infrastructureLogged: InfrastructureLogged,
    ): TeamCityMessageCommonClient {
        val client = WebpackLogClient(clientType, logger, infrastructureLogged)

        if (progressLogger != null) {
            client.progressLogger = progressLogger
        }

        return client
    }

    private fun configureExec(
        execHandleBuilder: ExecHandleBuilder,
        progressLogger: ProgressLogger?,
    ): Pair<TeamCityMessageCommonClient, TeamCityMessageCommonClient> {
        check(config.entry?.isFile == true) {
            "${this}: Entry file does not exist \"${config.entry}\""
        }

        val infrastructureLogged = InfrastructureLogged(false)

        val standardClient = configureClient(LogType.LOG, progressLogger, infrastructureLogged)
        execHandleBuilder.standardOutput = TCServiceMessageOutputStreamHandler(
            client = standardClient,
            onException = { },
            logger = standardClient.log
        )

        val errorClient = configureClient(LogType.ERROR, progressLogger, infrastructureLogged)
        execHandleBuilder.errorOutput = TCServiceMessageOutputStreamHandler(
            client = errorClient,
            onException = { },
            logger = errorClient.log
        )

        config.save(configFile)

        val args = buildArgs()

        npmProject.useTool(
            execHandleBuilder = execHandleBuilder,
            tool = tool,
            nodeArgs = nodeArgs,
            args = args,
        )

        return standardClient to errorClient
    }

    private fun buildArgs(): List<String> {
        val args = args.toMutableList()

        args.add("--config")
        args.add(configFile.absolutePath)
        if (config.showProgress) {
            args.add("--progress")
        }

        return args
    }
}
