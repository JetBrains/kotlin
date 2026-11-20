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
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString
import kotlin.reflect.KProperty

class LocalClaudeAgent private constructor(
    private val project: LocalProject,
    private val authMethod: AuthMethod,
    private val command: List<String>,
) : Agent {
    sealed class AuthMethod {
        /**
         * Use the authentication configured for Claude Code in `~/.claude/settings.json` or environment variables.
         */
        object LocalConfiguration : AuthMethod()

        /**
         * Run Claude Code with `central run`, which provides the authentication.
         * See https://www.jetbrains.com/help/central-cli/cli-agents.html.
         */
        class JetBrainsCentralRun(val binary: String) : AuthMethod()
    }

    companion object {
        suspend fun create(project: LocalProject, authMethod: AuthMethod): LocalClaudeAgent =
            LocalClaudeAgent(project, authMethod, createCommand(authMethod))

        private val json = Json {
            ignoreUnknownKeys = true
        }

        /**
         * The value for `--model` when calling `claude`.
         *
         * The current mid-tier model. Per Anthropic's model documentation, Sonnet is
         * "the best combination of speed and intelligence" — the stronger tiers are
         * positioned for demanding or long-running work, and the cheaper tier for
         * high-volume, latency-sensitive tasks. See
         * https://platform.claude.com/docs/en/models/overview
         */
        private const val MODEL = "claude-sonnet-5-5"

        /**
         * The value for `--effort` in when calling `claude`.
         *
         * `medium` is the effort level recommended for this model for well-specified
         * agentic coding and multistep tool use, and a per-rule review invocation is
         * exactly that: one rule, one diff, a fixed output schema. `high` is recommended
         * only for harder or longer tasks, which these invocations are not. See
         * https://platform.claude.com/docs/en/build-with-claude/effort#recommended-effort-levels-for-claude-sonnet-5-5
         *
         * Coincidentally, `medium` is the default effort level in Claude Code for this model at the moment of writing.
         * But it may change with CLI updates or account/organization configuration. Pin the level to keep it deterministic.
         */
        private const val EFFORT = "medium"

        private suspend fun createCommand(authMethod: AuthMethod): List<String> = buildList {
            val prompt = """
                The input contains a code rule and the path to a file with a git diff of files subject to the rule.
                Your task is to check whether this diff meets this rule, and report any violations.
                Don't report problems that are pre-existing (i.e. not introduced with this diff).
                If a change violates a rule but explains in the comment why the violation should be allowed, don't report it.
                Read and search the diff file in the way that suits this rule best: read it fully
                if the rule requires the whole diff, or search it for the specific things the rule is about.
                You can also read any files in the repository. It is available at the current working directory
                and already has the patch applied.
            """.trimIndent()

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

            when (authMethod) {
                AuthMethod.LocalConfiguration -> {
                    add("claude")

                    add("--settings")
                    add(json.encodeToString(getAuthSettings()))
                }
                is AuthMethod.JetBrainsCentralRun -> {
                    // `central run` provides the authentication to Claude Code through environment variables.
                    add(authMethod.binary)
                    add("run")
                    add("claude")
                    add("--")
                }
            }

            add("--bare")
            add("-p")
            add(prompt)

            add("--output-format")
            add("json")
            add("--json-schema")
            add(jsonSchemaString)

            add("--model")
            add(MODEL)
            add("--effort")
            add(EFFORT)

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
                authSettings.env?.anthropicAuthToken == null &&
                System.getenv(ANTHROPIC_AUTH_TOKEN) == null &&
                authSettings.apiKeyHelper == null &&
                authSettings.env?.anthropicApiKey == null &&
                System.getenv(ANTHROPIC_API_KEY) == null
            ) {
                error("No Anthropic API credentials found. See repo/auto-code-review/README.md for how to provide them.")
            }

            authSettings
        }

        private const val ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY"
        private const val ANTHROPIC_AUTH_TOKEN = "ANTHROPIC_AUTH_TOKEN"
    }

    override val info = AgentInfo("Claude Code", MODEL, EFFORT)

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
            @SerialName(ANTHROPIC_AUTH_TOKEN)
            val anthropicAuthToken: String? = null,
        )
    }

    override suspend fun checkRule(
        rule: CodeRule,
        changedFiles: List<GitDiff.ChangedFile>,
        diffOrigin: GitDiff.Origin,
    ): AgentResult {
        val diffDescription = when (diffOrigin) {
            is GitDiff.Origin.Local -> "`git diff ${diffOrigin.from.sha1}` at `${diffOrigin.to.root}`"
            is GitDiff.Origin.GitHub -> diffOrigin.rawDiffUrl
        }

        var executionResult: ExecutionResult? = null
        var output: ClaudeOutput? = null

        return runCatching {
            val runDir = createTempDirectory(prefix = "auto-code-review-")
            try {
                val diffFile = runDir.resolve("diff.diff")
                writeDiffFile(diffFile, changedFiles)
                val input = buildInput(rule, diffDescription, diffFile)

                // In the `dontAsk` permission mode, the agent is allowed to read only
                // the working directory and the added ones, so without this argument
                // it cannot read the diff.
                val addDirArguments = listOf("--add-dir", runDir.pathString)
                executionResult = runProcess(
                    directory = project.root,
                    input = input,
                    addEnvironment = mapOf(
                        // Don't read ~/.claude.json.
                        // See https://code.claude.com/docs/en/agent-sdk/claude-code-features#what-settingsources-does-not-control.
                        "CLAUDE_CONFIG_DIR" to runDir.resolve("config").pathString,

                        // Don't write prompt history to disk.
                        "CLAUDE_CODE_SKIP_PROMPT_HISTORY" to "1",
                    ),
                    command = command + addDirArguments,
                )
            } finally {
                @OptIn(ExperimentalPathApi::class)
                runDir.deleteRecursively()
            }
            val claudeStdout = when (authMethod) {
                AuthMethod.LocalConfiguration -> executionResult.stdout
                is AuthMethod.JetBrainsCentralRun -> {
                    // `central run` prints a message to stdout before running Claude Code.
                    // Reported as https://youtrack.jetbrains.com/issue/JCP-8529.
                    // Claude Code prints the JSON output as a single line, so take only the last line as a workaround.
                    executionResult.stdout.lines().last()
                }
            }
            output = json.decodeFromString<ClaudeOutput>(claudeStdout)
            executionResult.checkExitCode()
            check(!output.isError) { "Claude returned an error: is_error = true in the output" }
            check(output.type == "result" && output.subtype == "success") {
                "Claude didn't produce a successful result: type = ${output.type}, subtype = ${output.subtype}"
            }

            check(output.structuredOutput != null) { "Claude produced no structured_output" }

            AgentResult.Success(output.structuredOutput, output.buildMeta())
        }.getOrElse {
            AgentResult.Failure(
                meta = output?.buildMeta(),
                exception = it,
                executionResult = executionResult,
                output = output?.result
            )
        }
    }

    /**
     * Writes the diff of the files to [diffFile].
     *
     * The patch lines are separated with '\n' — the line separator of the git diff output they
     * come from — and not with the system line separator.
     */
    private suspend fun writeDiffFile(diffFile: Path, changedFiles: List<GitDiff.ChangedFile>) {
        withContext(Dispatchers.IO) {
            diffFile.bufferedWriter().use { writer ->
                changedFiles.flatMap { it.patchLines }.forEach(writer::appendLine)
            }
        }
    }

    private fun buildInput(
        rule: CodeRule,
        diffDescription: String,
        diffFile: Path,
    ): String = buildString {
        appendLine("# Rule: ${rule.name}")
        appendLine()
        appendLine(rule.text)
        appendLine(
            """
                
                # Diff:
                
                The full diff is available from $diffDescription. It may contain other changes.
                
                Check the rule only against the diff in this file: ${diffFile.absolutePathString()}
                It features files that are subject to the rule.
            """.trimIndent()
        )
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
        val structuredOutput: ReviewComments? = null,
    ) {
        fun buildMeta(): ReviewMeta = ReviewMeta(this.totalCostUSD)
    }
}

private inline fun <reified T> KProperty<T>.propertyTypeSerializer() = serializer<T>()
