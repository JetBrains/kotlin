/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString
import kotlin.reflect.KProperty

class LocalClaudeAgent private constructor(
    private val project: LocalProject,
    private val command: List<String>,
) : Agent {
    companion object {
        suspend fun create(project: LocalProject): LocalClaudeAgent =
            LocalClaudeAgent(project, createCommand())

        private val json = Json {
            ignoreUnknownKeys = true
        }

        private suspend fun createCommand(): List<String> = buildList {
            val prompt = """
                The input contains a code rule and a git diff of files subject to the rule.
                Your task is to check whether this diff meets this rule, and report any violations.
                Don't report problems that are pre-existing (i.e. not introduced with this diff).
                If a change violates a rule but explains in the comment why the violation should be allowed, don't report it.
                You can also read any files in the repository. It is available at the current working directory
                and already has the patch applied.
            """.trimIndent()

            val authSettingsString = json.encodeToString(getAuthSettings())

            val jsonSchema = SerializationClassJsonSchemaGenerator.Default
                .generateSchema(ClaudeOutput::structuredOutput.propertyTypeSerializer().descriptor)
                .copy(
                    /*
                    Anthropic API just silently ignores the schema and doesn't produce any structured output
                    if the schema has the `$schema` key.
                    The documentation doesn't mention this explicitly, though:
                    https://platform.claude.com/docs/en/build-with-claude/structured-outputs#json-schema-limitations.
                    Just remove this key from the schema as a workaround:
                     */
                    schema = null
                )

            val jsonSchemaString = Json.encodeToString(jsonSchema)

            val model = "claude-sonnet-4-6"

            add("claude")

            add("--settings")
            add(authSettingsString)

            add("--bare")
            add("-p")
            add(prompt)

            add("--output-format")
            add("json")
            add("--json-schema")
            add(jsonSchemaString)

            add("--model")
            add(model)

            // `dontAsk` means "allow only pre-approved tools, **don't ask** for more".
            add("--permission-mode")
            add("dontAsk")

            // Don't save sessions to disk.
            add("--no-session-persistence")

            // Only use MCP servers from --mcp-config (= none).
            add("--strict-mcp-config")

            // Don't load `CLAUDE.md`, `./.claude`, `~/.claude/` etc.
            // See https://code.claude.com/docs/en/agent-sdk/claude-code-features#control-filesystem-settings-with-settingsources.
            add("--setting-sources")
            add("")
        }

        private suspend fun getAuthSettings(): AuthSettings = withContext(Dispatchers.IO) {
            val allSettingsFile = File(System.getProperty("user.home")!!, ".claude/settings.json")

            val authSettings = if (allSettingsFile.exists()) {
                // Basically, sanitize the settings by keeping only the ones defined in `AuthSettings`.
                json.decodeFromString<AuthSettings>(allSettingsFile.readText())
            } else {
                AuthSettings()
            }

            if (
                authSettings.apiKeyHelper == null &&
                authSettings.env?.anthropicApiKey == null &&
                System.getenv(ANTHROPIC_API_KEY) == null
            ) {
                error(
                    """
                        No Anthropic API key found in environment or ${allSettingsFile.toURI().toURL()}.
                        Please make sure that either "$ANTHROPIC_API_KEY" or "apiKeyHelper" is defined.
                    """.trimIndent()
                )
            }

            authSettings
        }

        private const val ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY"
    }

    @Serializable
    private class AuthSettings(
        val apiKeyHelper: String? = null,
        val env: Env? = null
    ) {
        @Serializable
        class Env(
            @SerialName("ANTHROPIC_BASE_URL")
            val anthropicBaseUrl: String? = null,
            @SerialName(ANTHROPIC_API_KEY)
            val anthropicApiKey: String? = null,
        )
    }

    override suspend fun checkRule(
        rule: CodeRule,
        changedFiles: List<GitDiff.ChangedFile>,
        diffOrigin: GitDiff.Origin,
    ): AgentResult {
        val diffDescription = when (diffOrigin) {
            is GitDiff.Origin.Local -> "`git diff ${diffOrigin.from.sha1}` at `${diffOrigin.to.root}`"
        }

        val input = buildString {
            appendLine("# Rule: ${rule.name}")
            appendLine()
            appendLine(rule.text)
            appendLine(
                """
                    
                    # Diff:
                    
                    The full diff is available from $diffDescription.
                    
                    Here is the part featuring the files that are subject to the rule:
                    
                    ```diff
                """.trimIndent()
            )
            changedFiles.forEach { it.patchLines.forEach(::appendLine) }
            appendLine("```")
        }

        var executionResult: ExecutionResult? = null
        var output: ClaudeOutput? = null

        return runCatching {
            val temporaryConfigDir = createTempDirectory(prefix = "claude-config")
            try {
                executionResult = runProcess(
                    directory = project.root,
                    input = input,
                    addEnvironment = mapOf(
                        // Don't read ~/.claude.json.
                        // See https://code.claude.com/docs/en/agent-sdk/claude-code-features#what-settingsources-does-not-control.
                        "CLAUDE_CONFIG_DIR" to temporaryConfigDir.pathString,

                        // Don't write prompt history to disk.
                        "CLAUDE_CODE_SKIP_PROMPT_HISTORY" to "1",
                    ),
                    command = command
                )
            } finally {
                @OptIn(ExperimentalPathApi::class)
                temporaryConfigDir.deleteRecursively()
            }
            output = json.decodeFromString<ClaudeOutput>(executionResult.stdout)
            executionResult.checkExitCode()
            check(!output.isError) { "Claude returned an error: is_error = true in the output" }
            check(output.type == "result" && output.subtype == "success") {
                "Claude didn't produce a successful result: type = ${output.type}, subtype = ${output.subtype}"
            }

            check(output.structuredOutput != null) { "Claude produced no structured_output" }

            AgentResult.Success(rule, output.structuredOutput, output.buildMeta())
        }.getOrElse {
            AgentResult.Failure(
                rule = rule,
                meta = output?.buildMeta(),
                exception = it,
                executionResult = executionResult,
                output = output?.result
            )
        }
    }

    @Serializable
    private class ClaudeOutput(
        val type: String,
        val subtype: String,
        val result: String,
        @SerialName("is_error")
        val isError: Boolean,
        @SerialName("total_cost_usd")
        val totalCostUSD: Double,
        @SerialName("structured_output")
        val structuredOutput: RuleReviewResult? = null,
    ) {
        fun buildMeta(): ReviewMeta = ReviewMeta(this.totalCostUSD)
    }
}

private inline fun <reified T> KProperty<T>.propertyTypeSerializer() = serializer<T>()
