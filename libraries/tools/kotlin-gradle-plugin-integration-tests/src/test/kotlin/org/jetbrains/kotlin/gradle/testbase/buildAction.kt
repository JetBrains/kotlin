package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult

typealias BuildAction = TestProject.(buildArguments: Array<String>, buildOptions: BuildOptions) -> Unit

object BuildActions {
    val build: BuildAction = { args, options ->
        build(
            buildArguments = args,
            buildOptions = options,
            forwardBuildOutput = false,
        )
    }

    fun buildWithAssertions(
        buildAssertions: BuildResult.() -> Unit = {},
    ): BuildAction = { args, options ->
        build(
            buildArguments = args,
            buildOptions = options,
            forwardBuildOutput = false,
            assertions = buildAssertions,
        )
    }

    val buildAndFail: BuildAction = { args, options ->
        buildAndFail(
            buildArguments = args,
            buildOptions = options,
            forwardBuildOutput = false,
        )
    }
}

