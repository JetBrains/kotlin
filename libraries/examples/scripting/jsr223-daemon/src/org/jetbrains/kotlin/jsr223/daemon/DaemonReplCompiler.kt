/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.daemon

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplSnippetConfigurationCodec
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import kotlin.script.experimental.api.*
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.jvm.impl.compiledSnippetFromClassPath
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

/**
 * A [ReplCompiler] that compiles snippets out-of-process through the daemon's ordinary compile path.
 * Each snippet represented as a plain `.repl.<extension>` source file, compiled to a plain `-d` output.
 *
 * Compiled snippets are wrapped by [compiledSnippetFromClassPath] into [kotlin.script.experimental.jvm.impl.KJvmCompiledScript], so
 * [K2ReplEvaluator][org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator] can evaluate them.
 *
 * [compile] runs [ScriptCompilationConfiguration.prependSyntheticSnippets] for every snippet, so a
 * definition wired through a synthetic-snippet-producing `refineConfiguration` handler (e.g.
 * `generateBindingSnippetIfNeeded`) gets its synthetic snippet(s) compiled ahead of the main one,
 * within the same [compile] call. There is no local FIR session here to run
 * `refineConfiguration`'s `beforeCompiling` hooks implicitly, so [compileSnippetBatch] runs
 * `refineBeforeCompiling` itself and sends the resulting configuration - including its
 * [ScriptCompilationConfiguration.implicitReceivers] - to the daemon as a serialized
 * `ScriptCompilationConfiguration` (`repl-snippet-configuration`).
 *
 * @param compilerClasspath classpath the compile daemon is identified with; must
 *   contain the Kotlin compiler plus the (unshaded) `kotlin-scripting-compiler` plugin jar.
 * @param additionalClasspath extra classpath entries every snippet is compiled against. Most
 *   importantly this should include the Kotlin stdlib, which the daemon compile does not add
 *   implicitly.
 * @param isDebugEnabled whether to surface the daemon's own debug-level connection report messages.
 * @param daemonJVMOptions the daemon's JVM options. When `null`, uses
 *   [KotlinCompilerRunnerUtils.newDaemonConnection]'s global default.
 * @param daemonOptions the daemon's own options (run-files directory, idle-shutdown delay, etc).
 *   When `null`, uses `newDaemonConnection`'s global default.
 * @param daemonLogOptions the daemon's log-file options. When `null`, uses
 *   `newDaemonConnection`'s global default.
 */
class DaemonReplCompiler(
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val isDebugEnabled: Boolean = false,
    private val daemonJVMOptions: DaemonJVMOptions? = null,
    private val daemonOptions: DaemonOptions? = null,
    private val daemonLogOptions: DaemonLogOptions? = null,
) : ReplCompiler<CompiledSnippet>, AutoCloseable {

    private val compilerId = CompilerId.makeCompilerId(compilerClasspath)
    private val sessionIsAliveFlagFile = makeAutodeletingFlagFile(keyword = "jsr223-daemon-session")

    private val workRoot = Files.createTempDirectory("jsr223-daemon-repl-work-").toFile()
    private var snippetCounter = 0

    private val messageCollector = CollectingMessageCollector()

    private val connectionLazy: Lazy<CompileServiceSession> = lazy {
        val effectiveDaemonOptions = daemonOptions ?: configureDaemonOptions()
        Files.createDirectories(File(effectiveDaemonOptions.runFilesPath).toPath())
        val effectiveLogOptions = daemonLogOptions ?: DaemonLogOptions()
        Files.createDirectories(File(effectiveLogOptions.logsPath).toPath())
        val effectiveJvmOptions = daemonJVMOptions ?: configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        )

        KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            messageCollector,
            isDebugEnabled = isDebugEnabled,
            daemonOptions = effectiveDaemonOptions,
            daemonJVMOptions = effectiveJvmOptions,
            daemonLogOptions = effectiveLogOptions,
        ) ?: throw IllegalStateException(
            "Could not connect to the Kotlin daemon for REPL snippet compilation:\n" +
                    messageCollector.messages.joinToString("\n")
        )
    }
    private val connection: CompileServiceSession by connectionLazy

    private val priorOutputDirs = mutableListOf<File>()
    private val priorClassIds = mutableListOf<ClassId>()

    private var lastCompiledSnippetInternal: LinkedSnippetImpl<CompiledSnippet>? = null

    override val lastCompiledSnippet: LinkedSnippet<CompiledSnippet>?
        get() = lastCompiledSnippetInternal

    override suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration,
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        val reports = mutableListOf<ScriptDiagnostic>()
        for (mainSnippet in snippets) {
            val [updatedConfiguration, syntheticSnippets] =
                configuration.prependSyntheticSnippets(mainSnippet).valueOr { return it }

            val batch = buildList {
                for (syntheticSnippet in syntheticSnippets) {
                    val syntheticConfiguration = updatedConfiguration.with {
                        resultField("")
                        repl.resultFieldPrefix("")
                        repl._isSyntheticSnippet(true)
                    }
                    add(syntheticSnippet to syntheticConfiguration)
                }
                add(mainSnippet to updatedConfiguration.with { reset(repl._isSyntheticSnippet) })
            }
            compileSnippetBatch(batch, reports).valueOr { return it }
        }
        return lastCompiledSnippetInternal?.asSuccess(reports)
            ?: ResultWithDiagnostics.Failure("No snippets provided".asErrorDiagnostics())
    }

    /**
     * Compiles a whole batch of physical snippets (a synthetic-snippet-producing definition's
     * synthetic snippet(s), followed by the main snippet they were generated for) through a single
     * [runDaemonCompile] call, as multiple source-root files of the same compile. Every compiled
     * snippet is then chained into [priorOutputDirs]/[priorClassIds]/[lastCompiledSnippetInternal].
     */
    private fun compileSnippetBatch(
        batch: List<Pair<SourceCode, ScriptCompilationConfiguration>>,
        reports: MutableList<ScriptDiagnostic>,
    ): ResultWithDiagnostics<Unit> {
        data class RefinedSnippet(val snippet: SourceCode, val name: String, val configuration: ScriptCompilationConfiguration)

        val refinedSnippets = batch.map { [snippet, snippetConfiguration] ->
            val snippetName = snippet.name
                ?: return ResultWithDiagnostics.Failure("DaemonReplCompiler: snippet has no name".asErrorDiagnostics())
            val refinedConfiguration = snippetConfiguration.refineBeforeCompiling(snippet).valueOr { return it }
            RefinedSnippet(snippet, snippetName, refinedConfiguration)
        }
        val batchBaseConfiguration = refinedSnippets.last().configuration
        val missingImplicitReceiverTypes = refinedSnippets
            .flatMap { it.configuration[ScriptCompilationConfiguration.implicitReceivers].orEmpty() }
            .distinct() - batchBaseConfiguration[ScriptCompilationConfiguration.implicitReceivers].orEmpty().toSet()
        val batchConfiguration = ScriptCompilationConfiguration(batchBaseConfiguration) {
            if (missingImplicitReceiverTypes.isNotEmpty()) {
                implicitReceivers(*missingImplicitReceiverTypes.toTypedArray())
            }
        }

        val batchIndex = snippetCounter++
        val outputDir = File(workRoot, "snippet-batch-$batchIndex-out").also { it.mkdirs() }
        val configurationFile = File(workRoot, "snippet-batch-$batchIndex-configuration.bin").also {
            ReplSnippetConfigurationCodec.writeTo(batchConfiguration, it)
        }
        val sourceDir = Files.createTempDirectory("jsr223-daemon-repl-snippet-src-").toFile()
        try {
            val scriptFiles = refinedSnippets.map { File(sourceDir, it.name).also { file -> file.writeText(it.snippet.text) } }
            val arguments = buildBatchCompilerArguments(scriptFiles, priorOutputDirs, priorClassIds, outputDir, configurationFile)

            messageCollector.clear()
            val exitCode = runDaemonCompile(arguments)
            if (exitCode != ExitCode.OK.code) {
                return ResultWithDiagnostics.Failure(
                    messageCollector.messages.map { it.asErrorDiagnostics(path = refinedSnippets.last().snippet.locationId) }
                )
            }

            priorOutputDirs += outputDir
            for ([refinedSnippet, scriptFile] in refinedSnippets.zip(scriptFiles)) {
                val classId = snippetClassId(scriptFile)
                val compiledSnippet = compiledSnippetFromClassPath(
                    classPath = listOf(outputDir),
                    snippetClassFQName = classId.asSingleFqName().asString(),
                    snippet = refinedSnippet.snippet,
                    compilationConfiguration = refinedSnippet.configuration,
                )
                priorClassIds += classId
                lastCompiledSnippetInternal = lastCompiledSnippetInternal.add(compiledSnippet)
            }
            reports += messageCollector.messages.map {
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedInfo, it, ScriptDiagnostic.Severity.WARNING,
                    refinedSnippets.last().snippet.locationId
                )
            }
            return Unit.asSuccess()
        } finally {
            sourceDir.deleteRecursively()
        }
    }

    private fun buildBatchCompilerArguments(
        scriptFiles: List<File>,
        priorOutputDirs: List<File>,
        priorClassIds: List<ClassId>,
        outputDir: File,
        configurationFile: File,
    ): List<String> {
        fun pluginOption(name: String, value: String) = "plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:$name=$value"
        return buildList {
            val classpathEntries = additionalClasspath.map { it.toAbsolutePath().toString() } + priorOutputDirs.map { it.absolutePath }
            if (classpathEntries.isNotEmpty()) {
                add("-cp")
                add(classpathEntries.joinToString(File.pathSeparator))
            }
            add("-Xallow-any-scripts-in-source-roots")
            add("-Xuse-fir-lt=false") // TODO: remove after finishing KT-77583
            add("-P")
            add(pluginOption("repl-snippet-stateless-mode", "true"))
            // Only the immediately preceding snippet: the compiler recovers the rest of the session by
            // following the prior-snippet links in the compiled snippets' metadata.
            priorClassIds.lastOrNull()?.let { classId ->
                add("-P")
                add(pluginOption("repl-snippet-prior-class", classId.asString()))
            }
            add("-P")
            add(pluginOption("repl-snippet-configuration", configurationFile.absolutePath))
            add("-d")
            add(outputDir.absolutePath)
            add("-Xsuppress-version-warnings")
            for (scriptFile in scriptFiles) {
                add(scriptFile.absolutePath)
            }
        }
    }

    private fun runDaemonCompile(arguments: List<String>): Int {
        val daemon = connection.compileService
        val sessionId = connection.sessionId

        val compilationOptions = CompilationOptions(
            compilerMode = NON_INCREMENTAL_COMPILER,
            targetPlatform = JVM,
            reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code),
            reportSeverity = ReportSeverity.INFO.code,
            requestedCompilationResults = emptyArray(),
        )

        return daemon.compile(
            sessionId,
            arguments.toTypedArray(),
            compilationOptions,
            BasicCompilerServicesWithResultsFacadeServer(messageCollector),
            NoOpCompilationResults(),
        ).get()
    }

    override fun close() {
        if (connectionLazy.isInitialized()) {
            try {
                connection.compileService.releaseCompileSession(connection.sessionId)
            } catch (_: RemoteException) {
                // The daemon might already be down; nothing more to release.
            }
        }
        workRoot.deleteRecursively()
    }

    @TestOnly
    fun forceShutdownDaemon() {
        if (!connectionLazy.isInitialized()) return
        try {
            connection.compileService.shutdown()
        } catch (_: RemoteException) {
            // The daemon might already be down.
        }
        Thread.sleep(500) // wait a bit so that the daemon is actually shut down
    }

    private fun snippetClassId(scriptFile: File): ClassId =
        ClassId(ROOT, NameUtils.getSnippetTargetClassName(scriptFile.name))

    companion object {
        private val clientIsAliveFile: File by lazy { makeAutodeletingFlagFile(keyword = "jsr223-daemon-client") }
    }
}

private class NoOpCompilationResults : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory,
    ) {
    override fun add(compilationResultCategory: Int, value: Serializable) {}
}

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
        if (severity == ERROR || severity == EXCEPTION) {
            sawErrors = true
        }
        if (severity == LOGGING || severity == OUTPUT) return
        val locationSuffix = location?.let { " (${it.path}:${it.line}:${it.column})" } ?: ""
        collected.add("$severity: $message$locationSuffix")
    }
}
