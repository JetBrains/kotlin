/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
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
 * A [ReplCompiler] that compiles snippets out-of-process through the daemon's regular compile path.
 * Each snippet represented as a plain `.repl.<extension>` source file, compiled to a plain `-d` output.
 *
 * Compiled snippets are wrapped by [compiledSnippetFromClassPath] into [kotlin.script.experimental.jvm.impl.KJvmCompiledScript], so
 * [K2ReplEvaluator][org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator] can evaluate them.
 *
 * There is no REPL-specific RMI or artifact protocol. A snippet compiles through a plain
 * `CompileService.compile(...)` call, switched into chained-REPL-snippet mode by scripting-plugin
 * CLI options (`repl-snippet-regular-mode`/`repl-snippet-prior-class`, see
 * `ScriptingCommandLineProcessor`). Previous snippets are fed back purely via the regular classpath
 * (their `-d` output directories) plus their [ClassId]s. [snippetClassId] predicts a snippet's
 * wrapper class name from its source file name ([NameUtils.getSnippetTargetClassName]), with no
 * round-trip needed. [compilerClasspath] must carry the plain, unshaded
 * `kotlin-scripting-compiler` jar so the daemon discovers the scripting plugin as a `kotlinc`
 * invocation would.
 *
 * A plain compile failure is reported as a [ResultWithDiagnostics.Failure], not a thrown exception.
 * Exceptions are reserved for precondition and infrastructure errors (daemon connection failure).
 *
 * ### Synthetic snippets and implicit receivers
 *
 * [compile] runs [ScriptCompilationConfiguration.prependSyntheticSnippets] for every snippet, so a
 * definition wired through a synthetic-snippet-producing `refineConfiguration` handler (e.g.
 * `generateBindingSnippetIfNeeded`) gets its synthetic snippet(s) compiled ahead of the main one,
 * within the same [compile] call. There is no local FIR session here to run
 * `refineConfiguration`'s `beforeCompiling` hooks implicitly, so [compileSnippetBatch] runs
 * `refineBeforeCompiling` itself and forwards the resulting
 * [ScriptCompilationConfiguration.implicitReceivers] type names to the daemon via a dedicated,
 * repeatable CLI option (`repl-snippet-implicit-receiver`). Without that, a snippet referring to
 * such a receiver unqualified (e.g. an exposed `ScriptContext`) would fail to compile. The matching
 * receiver *instances* are supplied purely client-side, at evaluation time, through
 * [K2ReplEvaluator]'s own `refineBeforeEvaluation` handling.
 *
 * ### Daemon connection lifecycle
 *
 * The daemon connection is created lazily on the first [compile] call and cached for this
 * compiler's whole lifetime. Re-leasing it per snippet would race the daemon's own idle-shutdown
 * timer. [close] releases that session, letting the daemon decide — per its own `daemonOptions`
 * idle-shutdown settings — when to actually exit, since it may be shared with other clients.
 * [forceShutdownDaemon] is a test-only escape hatch that kills the daemon process outright,
 * bypassing that wait.
 *
 * @param compilerClasspath classpath (jars) the compile daemon is spawned/identified with; must
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

    // Holds every compiled snippet's own `-d` output directory; must survive for this compiler's
    // whole lifetime since K2ReplEvaluator's classloader chain keeps referencing them. Deleted in close().
    private val workRoot = Files.createTempDirectory("jsr223-daemon-repl-work-").toFile()
    private var snippetCounter = 0

    // Collects the daemon-reported compiler messages for the current compile. Reset before each
    // compile so messages don't leak into a later snippet's report. Also used as connectionLazy's
    // own MessageCollector.
    private val messageCollector = CollectingMessageCollector()

    // Lazily created and cached for this compiler's whole lifetime. See the class KDoc's "Daemon
    // connection lifecycle" section.
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

    // The `-d` output directory and ClassId of every snippet compiled so far. Fed back to the
    // daemon as classpath entries plus `repl-snippet-prior-class`es so it can resolve cross-snippet
    // references. Distinct from lastCompiledSnippet, which K2ReplEvaluator walks to *run* snippets.
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
            // Generates any bindings-exposing (or other) synthetic snippet(s) client-side. See
            // ScriptCompilationConfiguration.prependSyntheticSnippets's KDoc.
            val [updatedConfiguration, syntheticSnippets] =
                configuration.prependSyntheticSnippets(mainSnippet).valueOr { return it }

            // Every synthetic snippet, plus the main snippet itself, is compiled together as one
            // batch. See compileSnippetBatch's KDoc for why.
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
     *
     * They must be compiled together. A same-batch sibling that comes before another one in [batch]
     * has no compiled bytecode yet at that point (codegen only happens once, after every file's body
     * is resolved), so it is resolved through [ClasspathBackedFirReplHistoryProvider]'s live,
     * same-session tracking rather than via [priorClassIds] and the classpath. Splitting them into
     * separate calls would make that resolution impossible.
     */
    private fun compileSnippetBatch(
        batch: List<Pair<SourceCode, ScriptCompilationConfiguration>>,
        reports: MutableList<ScriptDiagnostic>,
    ): ResultWithDiagnostics<Unit> {
        // Runs beforeCompiling refineConfiguration hooks (e.g. configureExposedJsr223Context)
        // client-side: there is no local FIR session here to run them implicitly. The one effect
        // that matters on this path is implicitReceivers. A single combined, de-duplicated list is
        // handed to the daemon (see buildBatchCompilerArguments) so the compiled classes actually
        // declare matching receiver parameters.
        data class RefinedSnippet(val snippet: SourceCode, val name: String, val configuration: ScriptCompilationConfiguration)

        val refinedSnippets = batch.map { [snippet, snippetConfiguration] ->
            val snippetName = snippet.name
                ?: return ResultWithDiagnostics.Failure("DaemonReplCompiler: snippet has no name".asErrorDiagnostics())
            val refinedConfiguration = snippetConfiguration.refineBeforeCompiling(snippet).valueOr { return it }
            RefinedSnippet(snippet, snippetName, refinedConfiguration)
        }
        val implicitReceiverTypeNames = refinedSnippets
            .flatMap { it.configuration[ScriptCompilationConfiguration.implicitReceivers].orEmpty().map { r -> r.typeName } }
            .distinct()

        // Every snippet of the batch shares the same base ScriptCompilationConfiguration, so its
        // fileExtension (the plain "kts" default, or e.g. "main.kts" for MainKtsScript) is identical
        // for all of them. It is forwarded to the daemon via a dedicated CLI option instead of a
        // hardcoded ".repl.kts" literal.
        val fileExtension = refinedSnippets.first().configuration[ScriptCompilationConfiguration.fileExtension] ?: "kts"

        // The `-d` output directory must survive past this compile call: it backs the classloader
        // K2ReplEvaluator uses to run the snippets, and any later snippet's compile. It lives under
        // workRoot and is only cleaned up in close().
        val outputDir = File(workRoot, "snippet-batch-${snippetCounter++}-out").also { it.mkdirs() }
        val sourceDir = Files.createTempDirectory("jsr223-daemon-repl-snippet-src-").toFile()
        try {
            // See buildBatchCompilerArguments's KDoc for why each snippet is written to its own
            // source file rather than passed via `-expression`/`-script`.
            val scriptFiles = refinedSnippets.map { File(sourceDir, it.name).also { file -> file.writeText(it.snippet.text) } }
            val arguments =
                buildBatchCompilerArguments(scriptFiles, priorOutputDirs, priorClassIds, outputDir, implicitReceiverTypeNames, fileExtension)

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
            // messageCollector's own (non-error) messages, e.g. warnings, are surfaced here as
            // reports on the successful result rather than silently dropped.
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

    /**
     * Builds the CLI argument list that compiles every one of [scriptFiles] as chained REPL
     * snippets on the regular compile path, all within one compile. The shape matches a CLI
     * `kotlinc a.repl.kts b.repl.kts ...` call.
     *
     * Each snippet's source is a plain source-root file, not `-script`/`-expression` (enabled by
     * `-Xallow-any-scripts-in-source-roots`). Both of those route through
     * `ScriptEvaluationExtension.eval()`, the same entry point that *runs* a script, so a plain
     * source-root file is the only way to guarantee a snippet that throws can never risk running
     * inside the daemon.
     *
     * [scriptFiles] entries are named `snippet_N.repl.<extension>` (see
     * [KotlinJsr223DaemonScriptEngineImpl]), which is what makes
     * `ScriptingProcessSourcesBeforeCompilingExtension` recognize them as REPL snippets and match
     * the `.repl.<extension>`-scoped `ScriptDefinition`. [fileExtension] forwards the same,
     * non-hardcoded extension to the daemon.
     *
     * [outputDir] backs `-d`. [priorOutputDirs] (from earlier compile calls, not this batch) go on
     * the classpath, and [priorClassIds] tells `ClasspathBackedFirReplHistoryProvider` which of
     * those classes are previous snippets (`repl-snippet-prior-class`). This batch's own snippets
     * resolve against each other through that provider's live, same-session tracking instead.
     *
     * `-Xuse-fir-lt=false` is required: `ScriptingProcessSourcesBeforeCompilingExtension` only runs
     * on the PSI-based source-collection path, not light-tree mode (the JVM pipeline's default).
     * With light-tree mode a snippet would silently compile as a plain, unmarked script instead.
     * This flag is deprecated and the piece of this design most likely to need revisiting.
     */
    private fun buildBatchCompilerArguments(
        scriptFiles: List<File>,
        priorOutputDirs: List<File>,
        priorClassIds: List<ClassId>,
        outputDir: File,
        implicitReceiverTypeNames: List<String>,
        fileExtension: String,
    ): List<String> {
        fun pluginOption(name: String, value: String) = "plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:$name=$value"
        return buildList {
            val classpathEntries = additionalClasspath.map { it.toAbsolutePath().toString() } + priorOutputDirs.map { it.absolutePath }
            if (classpathEntries.isNotEmpty()) {
                add("-cp")
                add(classpathEntries.joinToString(File.pathSeparator))
            }
            add("-Xallow-any-scripts-in-source-roots")
            add("-Xuse-fir-lt=false")
            add("-P")
            add(pluginOption("repl-snippet-regular-mode", "true"))
            for (classId in priorClassIds) {
                add("-P")
                add(pluginOption("repl-snippet-prior-class", classId.asString()))
            }
            // See compileSnippetBatch's KDoc for where this list comes from. It reaches the daemon so
            // compiled snippet classes declare a matching implicit-receiver parameter.
            for (implicitReceiverTypeName in implicitReceiverTypeNames) {
                add("-P")
                add(pluginOption("repl-snippet-implicit-receiver", implicitReceiverTypeName))
            }
            // Not hardcoded to "kts": lets ScriptingProcessSourcesBeforeCompilingExtension and the
            // fallback ScriptDefinition in pluginRegisrar.kt recognize this batch's own extension.
            add("-P")
            add(pluginOption("repl-snippet-file-extension", fileExtension))
            add("-d")
            add(outputDir.absolutePath)
            add("-Xsuppress-version-warnings")
            for (scriptFile in scriptFiles) {
                add(scriptFile.absolutePath)
            }
        }
    }

    /** Runs a single non-incremental compile on the (lazily created, cached) daemon connection. */
    private fun runDaemonCompile(arguments: List<String>): Int {
        val daemon = connection.compileService
        val sessionId = connection.sessionId

        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
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

    /**
     * Releases the cached compile-daemon session, if one was ever created, and deletes [workRoot].
     * The daemon may be shared with other clients, so it decides when to exit according to its own
     * idle-shutdown settings. See [forceShutdownDaemon] for a test-only alternative that skips that
     * wait.
     */
    override fun close() {
        if (connectionLazy.isInitialized()) {
            try {
                connection.compileService.releaseCompileSession(connection.sessionId)
            } catch (e: RemoteException) {
                // The daemon might already be down; nothing more to release.
            }
        }
        workRoot.deleteRecursively()
    }

    /**
     * Test-only hook: shuts the compile-daemon process down immediately, bypassing idle-shutdown
     * entirely. Not meant for production use; an embedder sharing the daemon with other clients
     * should use [close] instead.
     *
     * `CompileService.shutdown()` only schedules the daemon's exit and returns before it actually
     * happens, so this also waits a bit for the process to go away. Without that wait, a JUnit
     * `@TempDir` for the daemon's run-files/logs directory can be deleted while the daemon is still
     * exiting and holding its log file open, which is a flaky failure on Windows.
     */
    @TestOnly
    fun forceShutdownDaemon() {
        if (!connectionLazy.isInitialized()) return
        try {
            connection.compileService.shutdown()
        } catch (e: RemoteException) {
            // The daemon might already be down.
        }
        Thread.sleep(500) // wait a bit so that the daemon is actually shut down
    }

    /**
     * Predicts the [ClassId] a `.repl.<extension>` [scriptFile] compiles to, with no round-trip to
     * the compiler. The FIR REPL-snippet builder derives it purely from the source file's own name
     * via [NameUtils.getSnippetTargetClassName], in the root package since [scriptFile] has no
     * package declaration.
     */
    private fun snippetClassId(scriptFile: File): ClassId =
        ClassId(FqName.ROOT, NameUtils.getSnippetTargetClassName(scriptFile.name))

    companion object {
        // One "client is alive" flag file per JVM, shared by every DaemonReplCompiler instance in it.
        private val clientIsAliveFile: File by lazy { makeAutodeletingFlagFile(keyword = "jsr223-daemon-client") }
    }
}

/**
 * A no-op [CompilationResults]. This module runs only non-incremental compiles, so there is nothing
 * to collect (no incremental-compilation iteration results, no build metrics).
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
 * A [MessageCollector] that captures the daemon-reported compiler messages as plain strings so
 * [DaemonReplCompiler] can return a structured diagnostics list on failure.
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
