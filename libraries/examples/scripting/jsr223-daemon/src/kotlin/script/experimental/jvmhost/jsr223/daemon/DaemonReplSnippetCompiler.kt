/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonOptions
import org.jetbrains.kotlin.daemon.common.makeAutodeletingFlagFile
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

/** Result of a [DaemonReplSnippetCompiler.compileSnippet] call. */
sealed class DaemonReplSnippetCompilationResult {
    /** [artifactBytes] is the wire-format artifact readable by [DaemonReplSnippetSession]. */
    class Success(val artifactBytes: ByteArray, val diagnostics: List<String>) : DaemonReplSnippetCompilationResult()
    class Failure(val diagnostics: List<String>) : DaemonReplSnippetCompilationResult()
}

/**
 * Compiles a stateless K2 REPL snippet **out-of-process**, on the Kotlin compile daemon's
 * **regular** compile path -- the exact same path the Build Tools API's
 * `CompileReplSnippetOperation` drives (see
 * `compiler/build-tools/kotlin-build-tools-impl/.../CompileReplSnippetOperationImpl.kt`), but
 * called directly through the daemon-client API instead of going through BTA.
 *
 * There is no REPL-specific RMI added to `CompileService`: the snippet rides a plain
 * `CompileService.compile(...)` call, switched into snippet mode by scripting-plugin options
 * (`repl-snippet-mode` / `repl-snippet-name` / `repl-snippet-prior-artifact` /
 * `repl-snippet-artifact-output`), exactly as a CLI invocation would (see
 * `ScriptingCommandLineProcessor`). Priors and the produced artifact are exchanged through plain
 * files.
 *
 * Unlike `CompileReplSnippetOperationImpl`, this class never has to synthesize a `-Xplugin`
 * services jar re-declaring the scripting compiler plugin: [compilerClasspath] is expected to
 * carry the **plain, unshaded** `kotlin-scripting-compiler` jar (with its own, un-relocated
 * `META-INF/services` entries), so the daemon discovers the plugin the same way a `kotlinc`
 * invocation would.
 *
 * A plain compile failure is **not** signalled by a thrown exception -- exceptions are reserved
 * for precondition/infra errors (daemon connection failure).
 *
 * @param compilerClasspath classpath (jars) the compile daemon is spawned/identified with; must
 *   contain the Kotlin compiler plus the (unshaded) `kotlin-scripting-compiler` plugin jar.
 * @param additionalClasspath extra classpath entries every snippet is compiled against -- most
 *   importantly the Kotlin stdlib.
 * @param daemonRunFilesPath directory for the daemon's run/lock files (see
 *   `DaemonOptions.runFilesPath`).
 * @param daemonLogsPath directory for the daemon's own log file.
 * @param shutdownDelayMilliseconds how long the daemon waits, after this session is released,
 *   before shutting itself down if idle.
 */
class DaemonReplSnippetCompiler(
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonRunFilesPath: File = File(System.getProperty("java.io.tmpdir"), "kotlin-jsr223-daemon-run"),
    private val daemonLogsPath: File = File(System.getProperty("java.io.tmpdir"), "kotlin-jsr223-daemon-logs"),
    private val shutdownDelayMilliseconds: Long = 0L,
    private val isDebugEnabled: Boolean = false,
) {
    private val compilerId = CompilerId.makeCompilerId(compilerClasspath)
    private val sessionIsAliveFlagFile = makeAutodeletingFlagFile(keyword = "jsr223-daemon-session")

    /**
     * Compiles [source] as a REPL snippet named [snippetName] against [priorArtifacts] (in
     * history order), returning either the produced artifact or the compile diagnostics.
     */
    fun compileSnippet(
        source: String,
        snippetName: String,
        priorArtifacts: List<ByteArray>,
    ): DaemonReplSnippetCompilationResult {
        val workDir = Files.createTempDirectory("jsr223-daemon-repl-snippet-").toFile()
        try {
            val priorFiles = priorArtifacts.mapIndexed { index, bytes ->
                File(workDir, "prior-$index.artifact").also { it.writeBytes(bytes) }
            }
            val outputFile = File(workDir, "snippet-out.artifact")
            // See buildSnippetCompilerArguments's KDoc for why the source is written to a temp file
            // (named after the snippet itself) and delivered as a plain source-root file rather than
            // smuggled through a `-expression` CLI argument or handed to `-script`.
            val scriptFile = File(workDir, snippetName).also { it.writeText(source) }
            val arguments = buildSnippetCompilerArguments(scriptFile, snippetName, priorFiles, outputFile)

            val messageCollector = CollectingMessageCollector()
            val exitCode = runDaemonCompile(messageCollector, arguments)

            return if (exitCode == ExitCode.OK.code && outputFile.exists()) {
                DaemonReplSnippetCompilationResult.Success(outputFile.readBytes(), messageCollector.messages)
            } else {
                DaemonReplSnippetCompilationResult.Failure(messageCollector.messages)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    /**
     * Builds the CLI argument list that compiles [scriptFile] as a stateless REPL snippet on the
     * regular compile path -- the same invocation shape a CLI `kotlinc` call would carry, so the
     * daemon needs no REPL-specific transport.
     *
     * The snippet source is written to [scriptFile] on disk and passed as a **plain source-root
     * file** (a free/positional argument, *not* `-script <path>` and *not* `-expression <source>`),
     * with `-Xallow-any-scripts-in-source-roots` letting a `.kts` file be accepted on that path.
     * Three reasons, in order of importance:
     *  * `-script`/`-expression` both route through `ScriptEvaluationExtension.eval()` -- the same
     *    entry point `kotlinc script.kts` uses to *run* a script. `REPL_SNIPPET_COMPILATION_MODE`
     *    short-circuits that entry before it ever evaluates anything (see `compileReplSnippet` in
     *    `AbstractScriptEvaluationExtension.kt`), but that safety is a *runtime flag check inside an
     *    evaluate-capable entry point*, not a structural guarantee -- a snippet that throws must
     *    never risk running inside the daemon. The plain source-root pipeline
     *    (`ScriptingProcessSourcesBeforeCompilingExtension`, this plugin's other
     *    `compileReplSnippet` call site) has no evaluation code path *at all*, so this risk cannot
     *    exist on it structurally, regardless of how any flag is set.
     *  * `-expression` hands the whole snippet body through as a single CLI argument string, which
     *    a sufficiently pathological source (very long, or containing characters that a given
     *    transport happens to be sensitive to) could in principle corrupt; a file path is always a
     *    short, plain string.
     *  * `-expression`/`-script` are both semantically "run this"; a plain source-root file is
     *    unambiguously "compile this", matching what this call actually does.
     *
     * [scriptFile] is named after [snippetName] itself, so `repl-snippet-name` is set purely for
     * documentation/robustness -- the file's own name already disambiguates the snippet.
     *
     * Two more flags are required to make this actually work:
     *  * `-Xuse-fir-lt=false` -- `ScriptingProcessSourcesBeforeCompilingExtension` (the plugin
     *    extension point that intercepts `REPL_SNIPPET_COMPILATION_MODE` sources on this pipeline,
     *    right alongside its pre-existing `-Xallow-any-scripts-in-source-roots` handling) only ever
     *    runs on the *PSI-based* `KotlinCoreEnvironment.getSourceFiles()` source-collection path.
     *    The JVM pipeline's *other*, default (`useLightTree = true`) source-collection path
     *    (`collectSources`) has no equivalent extension point at all, so with the default light-tree
     *    mode this source would be silently accepted and silently produce nothing -- no artifact, no
     *    diagnostic, and (misleadingly) exit code `0`. This flag is deprecated (scheduled for
     *    removal once light-tree mode becomes the compiler's *only* mode) and is the one piece of
     *    this design most likely to need revisiting in a future compiler version.
     *  * `-Xallow-no-source-files` -- once intercepted, the snippet is deliberately *not* handed to
     *    the regular frontend/backend (there is nothing for them to usefully do with a REPL
     *    snippet), so the source set the rest of the pipeline sees is empty; without this flag the
     *    JVM pipeline reports a hard "no source files" error for exactly that reason.
     */
    private fun buildSnippetCompilerArguments(
        scriptFile: File,
        snippetName: String,
        priorFiles: List<File>,
        outputFile: File,
    ): List<String> {
        fun pluginOption(name: String, value: String) = "plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:$name=$value"
        return buildList {
            if (additionalClasspath.isNotEmpty()) {
                add("-cp")
                add(additionalClasspath.joinToString(File.pathSeparator) { it.toAbsolutePath().toString() })
            }
            add("-Xallow-any-scripts-in-source-roots")
            add("-Xuse-fir-lt=false")
            add("-Xallow-no-source-files")
            add("-P")
            add(pluginOption("repl-snippet-mode", "true"))
            add("-P")
            add(pluginOption("repl-snippet-name", snippetName))
            for (priorFile in priorFiles) {
                add("-P")
                add(pluginOption("repl-snippet-prior-artifact", priorFile.absolutePath))
            }
            add("-P")
            add(pluginOption("repl-snippet-artifact-output", outputFile.absolutePath))
            add("-Xsuppress-version-warnings")
            add(scriptFile.absolutePath)
        }
    }

    /** Connects to (or starts) the compile daemon and runs a single non-incremental compile. */
    private fun runDaemonCompile(messageCollector: CollectingMessageCollector, arguments: List<String>): Int {
        Files.createDirectories(daemonLogsPath.toPath())
        Files.createDirectories(daemonRunFilesPath.toPath())

        val daemonLogOptions = DaemonLogOptions(logsPath = daemonLogsPath.absolutePath)
        val daemonOptions = configureDaemonOptions(
            DaemonOptions().apply {
                shutdownDelayMilliseconds = this@DaemonReplSnippetCompiler.shutdownDelayMilliseconds
                runFilesPath = daemonRunFilesPath.absolutePath
            }
        )
        val jvmOptions: DaemonJVMOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        )

        val connection = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            messageCollector,
            isDebugEnabled = isDebugEnabled,
            daemonJVMOptions = jvmOptions,
            daemonOptions = daemonOptions,
            daemonLogOptions = daemonLogOptions,
        ) ?: throw IllegalStateException(
            "Could not connect to the Kotlin daemon for REPL snippet compilation:\n" +
                    messageCollector.messages.joinToString("\n")
        )

        val daemon = connection.compileService
        val sessionId = connection.sessionId

        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code),
            reportSeverity = ReportSeverity.INFO.code,
            requestedCompilationResults = emptyArray(),
        )

        try {
            return daemon.compile(
                sessionId,
                arguments.toTypedArray(),
                compilationOptions,
                BasicCompilerServicesWithResultsFacadeServer(messageCollector),
                NoOpCompilationResults(),
            ).get()
        } finally {
            try {
                daemon.releaseCompileSession(sessionId)
            } catch (e: RemoteException) {
                // The daemon might already be down; nothing more to release.
            }
        }
    }

    companion object {
        // One "client is alive" flag file per JVM, shared by every DaemonReplSnippetCompiler
        // instance in it - mirrors CompileReplSnippetOperationImpl's `clientIsAliveFile`.
        private val clientIsAliveFile: File by lazy { makeAutodeletingFlagFile(keyword = "jsr223-daemon-client") }
    }
}

/**
 * A no-op [CompilationResults]: this module runs only non-incremental, single-snippet compiles, so
 * there is nothing to collect (no incremental-compilation iteration results, no build metrics).
 */
private class NoOpCompilationResults : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory,
    ) {
    override fun add(compilationResultCategory: Int, value: Serializable) {}
}

/**
 * A [MessageCollector] that captures the daemon-reported compiler messages as plain strings, so
 * [DaemonReplSnippetCompiler.compileSnippet] can return a structured diagnostics list on failure.
 */
private class CollectingMessageCollector : MessageCollector {
    private val collected = mutableListOf<String>()
    private var sawErrors = false

    val messages: List<String> get() = collected

    override fun clear() {
        collected.clear()
        sawErrors = false
    }

    override fun hasErrors(): Boolean = sawErrors

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity == CompilerMessageSeverity.ERROR || severity == CompilerMessageSeverity.EXCEPTION) {
            sawErrors = true
        }
        if (severity == CompilerMessageSeverity.LOGGING || severity == CompilerMessageSeverity.OUTPUT) return
        val locationSuffix = location?.let { " (${it.path}:${it.line}:${it.column})" } ?: ""
        collected.add("$severity: $message$locationSuffix")
    }
}
