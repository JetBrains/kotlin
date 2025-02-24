/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.internal.TeamCityMessageCommonClient
import org.jetbrains.kotlin.gradle.internal.execWithErrorLogger
import org.jetbrains.kotlin.gradle.internal.newBuildOpLogger
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle
import org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle.Companion.execAsync
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
    private val execOps: ExecOperations,
) {

    fun execute(): ExecResult {
        val progressLogger = objects.newBuildOpLogger()
        val (standardClient, errorClient) = createTeamCityClients(progressLogger)

        return execWithErrorLogger(
            progressLogger,
            description = "webpack",
            execOps = execOps,
            errorClient = errorClient,
            standardClient = standardClient,
        ) { execSpec ->
            configureExec(
                execSpec,
                standardClient,
                errorClient,
            )
        }
    }

    fun start(): ExecAsyncHandle {
        val (standardClient, errorClient) = createTeamCityClients(null)

        return execOps.execAsync(
            displayName = "webpack $tool ${npmProject.compilationName}"
        ) { execSpec ->
            configureExec(
                execSpec,
                standardClient,
                errorClient,
            )
        }.start()
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

    private fun createTeamCityClients(
        logger: ProgressLogger?,
    ): Pair<TeamCityMessageCommonClient, TeamCityMessageCommonClient> {
        val infrastructureLogged = InfrastructureLogged(false)
        val standardClient = configureClient(LogType.LOG, logger, infrastructureLogged)
        val errorClient = configureClient(LogType.ERROR, logger, infrastructureLogged)

        return standardClient to errorClient
    }

    private fun configureExec(
        execSpec: ExecSpec,
        errorClient: TeamCityMessageCommonClient,
        standardClient: TeamCityMessageCommonClient,
    ) {
        check(config.entry?.isFile == true) {
            "${this}: Entry file does not exist \"${config.entry}\""
        }

        execSpec.standardOutput = TCServiceMessageOutputStreamHandler(
            client = standardClient,
            onException = { },
            logger = standardClient.log
        )

        execSpec.errorOutput = TCServiceMessageOutputStreamHandler(
            client = errorClient,
            onException = { },
            logger = errorClient.log
        )

        config.save(configFile)

        val args = buildArgs()

        npmProject.useTool(
            exec = execSpec,
            tool = tool,
            nodeArgs = nodeArgs,
            args = args,
        )
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
