/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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