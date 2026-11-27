/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

class AgentInfo(val name: String, val model: String, val effort: String)

interface Agent {
    sealed class AuthMethod {
        /**
         * Use the authentication configured for the agent locally.
         */
        object LocalConfiguration : AuthMethod()

        /**
         * Run the agent with `central run`, which provides the authentication.
         * See https://www.jetbrains.com/help/central-cli/cli-agents.html.
         */
        class JetBrainsCentralRun(val binary: String) : AuthMethod()
    }

    val info: AgentInfo

    suspend fun checkRule(
        rule: CodeRule,
        changedFiles: List<GitDiff.ChangedFile>,
        diffOrigin: GitDiff.Origin,
    ): AgentResult
}

class AgentConfiguration(
    val authMethod: Agent.AuthMethod = Agent.AuthMethod.LocalConfiguration,
    val model: String? = null,
    val effort: String? = null,
) {
    companion object {

        fun fromSystemProperties(): AgentConfiguration = AgentConfiguration(
            authMethod = when (val jetbrainsCentralBinary = System.getProperty("kotlin.autoCodeReview.useJetBrainsCentralCLI")) {
                null -> Agent.AuthMethod.LocalConfiguration
                else -> Agent.AuthMethod.JetBrainsCentralRun(jetbrainsCentralBinary)
            },
            model = System.getProperty("kotlin.autoCodeReview.model"),
            effort = System.getProperty("kotlin.autoCodeReview.effort"),
        )
    }
}
