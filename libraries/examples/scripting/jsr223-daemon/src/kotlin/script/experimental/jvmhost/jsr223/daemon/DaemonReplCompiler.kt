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
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleFromClassPath
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

/**
 * A [ReplCompiler] that drives compilation through the compile daemon's **regular** compile path
 * -- a plain `.repl.kts` source-root file, compiled by the unmodified regular JVM frontend/backend
 * (enabled by `-Xallow-any-scripts-in-source-roots`) straight to a plain `-d` output -- instead of
 * the in-process [org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler].
 *
 * This is the *only* substitution [KotlinJsr223DaemonScriptEngineImpl] makes on top of the stock
 * REPL infrastructure: state/eval-loop management ([kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineBase])
 * and result evaluation ([org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator]) are
 * reused verbatim -- see that class's KDoc for the overall design rationale. Each snippet is
 * compiled straight to a **regular output directory** (`-d`, one per snippet, kept alive for this
 * compiler's whole lifetime) and wrapped into a real [KJvmCompiledScript] backed by a plain
 * [KJvmCompiledModuleFromClassPath] classloader over that directory -- no bespoke artifact
 * deserialization is involved -- so [K2ReplEvaluator] can evaluate it exactly as it would a snippet
 * compiled in-process, including its existing cross-snippet classloader chaining
 * ([kotlin.script.experimental.jvm.impl.getOrCreateActualClassloader]'s `lastSnippetClassLoader`
 * handling), which this class does not need to (and does not) reimplement.
 *
 * There is no REPL-specific RMI added to `CompileService`, and no artifact blob/header of any kind:
 * a snippet rides a plain `CompileService.compile(...)` call, switched into chained-REPL-snippet
 * mode by scripting-plugin options (`repl-snippet-regular-mode` / `repl-snippet-prior-class`),
 * exactly as a CLI invocation would (see `ScriptingCommandLineProcessor`). Priors are fed back
 * purely via the regular classpath (their `-d` output directories) plus their `ClassId`s -- the
 * compiler's FIR REPL machinery resolves their declarations straight from their compiled classes'
 * own embedded metadata (see `ClasspathBackedFirReplHistoryProvider`). [decodeCompiledSnippet]
 * predicts the wrapper class name from the source file name it wrote
 * ([NameUtils.getSnippetTargetClassName]) -- no round-trip needed to learn it.
 *
 * This class never has to synthesize a `-Xplugin` services jar re-declaring the scripting
 * compiler plugin: [compilerClasspath] is expected to carry the **plain, unshaded**
 * `kotlin-scripting-compiler` jar (with its own, un-relocated `META-INF/services` entries), so
 * the daemon discovers the plugin the same way a `kotlinc` invocation would.
 *
 * A plain compile failure is **not** signalled by a thrown exception -- exceptions are reserved
 * for precondition/infra errors (daemon connection failure).
 *
 * ### Daemon connection lifecycle
 *
 * The compile-daemon connection is created **lazily, once**, on the first [compile] call, and then
 * cached and reused for every subsequent snippet -- this is how a long-lived daemon client is
 * supposed to behave: a session is leased once and held for the client's whole lifetime, not
 * re-leased/released on every single request (which would otherwise make the daemon race its own
 * idle-shutdown timer between every snippet). [close] releases that session once this compiler is
 * no longer needed, letting the daemon decide -- per its own `daemonOptions` idle-shutdown
 * settings -- when to actually exit, since the daemon process may be shared with other, unrelated
 * clients. [forceShutdownDaemon] is a test-only escape hatch that kills the daemon process
 * outright, bypassing that wait -- see its KDoc.
 *
 * @param compilerClasspath classpath (jars) the compile daemon is spawned/identified with; must
 *   contain the Kotlin compiler plus the (unshaded) `kotlin-scripting-compiler` plugin jar.
 * @param additionalClasspath extra classpath entries every snippet is compiled against -- most
 *   importantly the Kotlin stdlib, which the daemon compile does not add implicitly.
 * @param isDebugEnabled whether to surface the daemon's own debug-level connection report messages.
 * @param daemonJVMOptions the daemon's JVM options (heap size, extra JVM args, etc). `null` (the
 *   default) reproduces [KotlinCompilerRunnerUtils.newDaemonConnection]'s own global default -- a
 *   [configureDaemonJVMOptions] call inheriting this process's memory limits and properties.
 * @param daemonOptions the daemon's own options (run-files directory, idle-shutdown delay, etc).
 *   `null` (the default) reproduces `newDaemonConnection`'s own global default ([configureDaemonOptions]).
 * @param daemonLogOptions the daemon's log-file options. `null` (the default) reproduces
 *   `newDaemonConnection`'s own global default (a plain [DaemonLogOptions]).
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

    // Root directory holding every compiled snippet's own `-d` output directory (see compile),
    // kept alive for this compiler's whole lifetime -- unlike a compile-time-only temp dir, these
    // must survive so their classes stay loadable for as long as the engine (and the classloader
    // chain K2ReplEvaluator builds over them) is in use. Deleted in close().
    private val workRoot = Files.createTempDirectory("jsr223-daemon-repl-work-").toFile()
    private var snippetCounter = 0

    // Collects the daemon-reported compiler messages for the snippet currently being compiled --
    // reset before every compile (see compile) so a later snippet's report never carries stale
    // messages left over from an earlier one. Also serves as connectionLazy's own MessageCollector
    // for its one-time daemon-connection setup.
    private val messageCollector = CollectingMessageCollector()

    // Lazily created on the first compile and cached for this compiler's whole lifetime -- see the
    // "Daemon connection lifecycle" section of the class KDoc. kotlin.lazy's default SYNCHRONIZED
    // mode provides all the thread-safety this needs.
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

    // The `-d` output directory and ClassId of every snippet compiled so far, in history order --
    // fed back to the daemon as classpath entries plus `repl-snippet-prior-class`es so it can
    // resolve cross-snippet references at compile time (see compile). Distinct from
    // lastCompiledSnippet below: that chain is what K2ReplEvaluator walks to *run* snippets; this
    // list is what the *compiler* needs to resolve *new* snippets against.
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
        for (snippet in snippets) {
            val snippetName = snippet.name
                ?: return ResultWithDiagnostics.Failure("DaemonReplCompiler: snippet has no name".asErrorDiagnostics())

            // The `-d` output directory: unlike the source-holding temp dir below, this one must
            // survive past this compile call -- it backs the KJvmCompiledModuleFromClassPath
            // classloader K2ReplEvaluator uses to run the snippet, and (as a prior) any later
            // snippet's compile -- so it lives under workRoot and is only cleaned up in close().
            val outputDir = File(workRoot, "snippet-${snippetCounter++}-out").also { it.mkdirs() }
            val sourceDir = Files.createTempDirectory("jsr223-daemon-repl-snippet-src-").toFile()
            try {
                // See buildSnippetCompilerArguments's KDoc for why the source is written to a temp
                // file (named after the snippet itself) and delivered as a plain source-root file
                // rather than smuggled through a `-expression` CLI argument or handed to `-script`.
                val scriptFile = File(sourceDir, snippetName).also { it.writeText(snippet.text) }
                val arguments = buildSnippetCompilerArguments(scriptFile, priorOutputDirs, priorClassIds, outputDir)

                messageCollector.clear()
                val exitCode = runDaemonCompile(arguments)
                if (exitCode != ExitCode.OK.code) {
                    return ResultWithDiagnostics.Failure(
                        messageCollector.messages.map { it.asErrorDiagnostics(path = snippet.locationId) }
                    )
                }

                val classId = snippetClassId(scriptFile)
                val compiledSnippet = decodeCompiledSnippet(outputDir, classId, snippet, configuration)
                priorOutputDirs += outputDir
                priorClassIds += classId
                lastCompiledSnippetInternal = lastCompiledSnippetInternal.add(compiledSnippet)
                // messageCollector's own (non-error) messages -- e.g. warnings -- are surfaced here
                // as reports on the successful result, rather than silently dropped.
                reports += messageCollector.messages.map {
                    ScriptDiagnostic(ScriptDiagnostic.unspecifiedInfo, it, ScriptDiagnostic.Severity.WARNING, snippet.locationId)
                }
            } finally {
                sourceDir.deleteRecursively()
            }
        }
        return lastCompiledSnippetInternal?.asSuccess(reports)
            ?: ResultWithDiagnostics.Failure("No snippets provided".asErrorDiagnostics())
    }

    /**
     * Builds the CLI argument list that compiles [scriptFile] as a **chained REPL snippet on the
     * regular compile path** -- the same invocation shape a CLI `kotlinc` call would carry, so the
     * daemon needs no REPL-specific transport, and the source flows unmodified into the regular
     * frontend/backend (see `ScriptingProcessSourcesBeforeCompilingExtension`).
     *
     * The snippet source is written to [scriptFile] on disk and passed as a **plain source-root
     * file** (a free/positional argument, *not* `-script <path>` and *not* `-expression <source>`),
     * with `-Xallow-any-scripts-in-source-roots` letting a `.kts` file be accepted on that path.
     * Three reasons, in order of importance:
     *  * `-script`/`-expression` both route through `ScriptEvaluationExtension.eval()` -- the same
     *    entry point `kotlinc script.kts` uses to *run* a script. The plain source-root pipeline
     *    has no evaluation code path *at all*, so a snippet that throws can never risk running
     *    inside the daemon, regardless of any flag.
     *  * `-expression` hands the whole snippet body through as a single CLI argument string, which
     *    a sufficiently pathological source (very long, or containing characters that a given
     *    transport happens to be sensitive to) could in principle corrupt; a file path is always a
     *    short, plain string.
     *  * `-expression`/`-script` are both semantically "run this"; a plain source-root file is
     *    unambiguously "compile this", matching what this call actually does.
     *
     * [scriptFile]'s name (`snippet_N.repl.kts`, see [KotlinJsr223DaemonScriptEngineImpl]) is what
     * makes `ScriptingProcessSourcesBeforeCompilingExtension` mark it as a REPL snippet, what makes
     * it match the `.repl.kts`-scoped `ScriptDefinition` (needed for the emitted result field, see
     * `pluginRegisrar.kt`), and what [snippetClassId] predicts its wrapper class from.
     *
     * [outputDir] is passed via the regular `-d` option, so the snippet's classes land as plain
     * `.class` files under it; [priorOutputDirs] (prior snippets' own such directories) are added
     * to the classpath, and [priorClassIds] tells the frontend's `ClasspathBackedFirReplHistoryProvider`
     * which classes among that classpath are the actual prior snippets (see
     * `repl-snippet-prior-class` in `ScriptingCommandLineProcessor`).
     *
     * `-Xuse-fir-lt=false` is still required: `ScriptingProcessSourcesBeforeCompilingExtension`
     * (the plugin extension point that marks REPL-snippet sources) only ever runs on the
     * *PSI-based* `KotlinCoreEnvironment.getSourceFiles()` source-collection path. The JVM
     * pipeline's *other*, default (`useLightTree = true`) source-collection path has no equivalent
     * extension point at all, so with the default light-tree mode the source would compile as a
     * plain, unmarked script instead. This flag is deprecated (scheduled for removal once
     * light-tree mode becomes the compiler's *only* mode) and is the one piece of this design most
     * likely to need revisiting in a future compiler version.
     */
    private fun buildSnippetCompilerArguments(
        scriptFile: File,
        priorOutputDirs: List<File>,
        priorClassIds: List<ClassId>,
        outputDir: File,
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
            add("-d")
            add(outputDir.absolutePath)
            add("-Xsuppress-version-warnings")
            add(scriptFile.absolutePath)
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
     * Releases the cached compile-daemon session, if one was ever created (a compiler that never
     * compiled anything never opened a connection, so this is then a no-op). Well-behaved
     * disposal: lets the daemon decide, per its own `daemonOptions.shutdownDelayMilliseconds`/
     * idle-shutdown settings, when to actually exit -- appropriate since the daemon process may be
     * shared with other, unrelated clients. Also deletes [workRoot], the root of every compiled
     * snippet's `-d` output directory -- safe once this compiler (and its owning engine) is no
     * longer needed; see [forceShutdownDaemon] for a test-only alternative that skips the daemon's
     * own wait.
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
     * Test-only hook: shuts the underlying compile-daemon process down immediately, bypassing
     * `daemonOptions.shutdownDelayMilliseconds`/idle-shutdown entirely -- mirrors
     * `BaseDaemonSessionTest.stopDaemons` (`compiler/daemon/daemon-tests`), so a test suite doesn't
     * have to wait out a real idle-shutdown delay for a clean daemon exit between runs. Not meant
     * for production use: an embedder sharing the daemon with other clients should use [close]
     * instead, which only releases this compiler's own session.
     *
     * `CompileService.shutdown()` is asynchronous -- it only schedules the daemon process's own
     * exit ([org.jetbrains.kotlin.daemon.CompileServiceImpl.shutdownWithDelay]) and returns before
     * that exit actually happens, so this also waits a bit for the process to actually go away,
     * exactly as `BaseDaemonSessionTest.stopDaemons` does. Without this, a caller relying on a
     * JUnit `@TempDir` for `daemonOptions.runFilesPath`/`daemonLogOptions.logsPath` can have JUnit
     * try to delete that directory while the daemon process is still exiting and holding its own
     * log file open -- harmless on most platforms, but a real (and otherwise flaky) failure on
     * Windows, where an open file cannot be deleted at all.
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
     * Predicts the [ClassId] a `.repl.kts` [scriptFile] compiles to, with **no round-trip to the
     * compiler needed**: `KtScript.replSnippetClassId` (the actual mechanism the FIR REPL-snippet
     * builder uses, see `PsiRawFirBuilder.kt`/`KtScript.kt`) derives it purely from the source
     * file's own name via [NameUtils.getSnippetTargetClassName], in the file's package -- always
     * the root package here, since [scriptFile] is written with no package declaration.
     */
    private fun snippetClassId(scriptFile: File): ClassId =
        ClassId(FqName.ROOT, NameUtils.getSnippetTargetClassName(scriptFile.name))

    /**
     * Wraps a compiled snippet's `-d` output directory into a [KJvmCompiledScript]: its classes are
     * loaded through a plain [KJvmCompiledModuleFromClassPath] classloader over [outputDir] --
     * regular `.class` files, no bespoke deserialization involved. [classId] (from [snippetClassId])
     * supplies the wrapper class name; the result field is always [RESULT_FIELD_NAME] -- the
     * `ScriptCompilationConfiguration.resultField` default that the `.repl.kts` `ScriptDefinition`
     * (see `pluginRegisrar.kt`) leaves untouched, so the compiled bytecode always emits a field
     * under exactly this name for the snippet's last-expression value.
     */
    private fun decodeCompiledSnippet(
        outputDir: File,
        classId: ClassId,
        snippet: SourceCode,
        configuration: ScriptCompilationConfiguration,
    ): KJvmCompiledScript {
        val compiledModule = KJvmCompiledModuleFromClassPath(listOf(outputDir))
        return KJvmCompiledScript(
            sourceLocationId = snippet.locationId,
            compilationConfiguration = configuration,
            scriptClassFQName = classId.asSingleFqName().asString(),
            resultField = RESULT_FIELD_NAME to KotlinType(RESULT_FIELD_TYPE_NAME),
            otherScripts = emptyList(),
            compiledModule = compiledModule,
        )
    }

    companion object {
        // The `ScriptCompilationConfigurationKeys.resultField` default (see `kotlin.script.experimental.api.resultField`),
        // left untouched by the `.repl.kts` ScriptDefinition -- so this is always the field name
        // FirReplSnippetConfiguratorExtensionImpl emits the snippet's last-expression value under.
        private const val RESULT_FIELD_NAME = "\$\$result"
        private const val RESULT_FIELD_TYPE_NAME = "kotlin.Any"

        // One "client is alive" flag file per JVM, shared by every DaemonReplCompiler instance in it.
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
 * [DaemonReplCompiler]'s daemon-compile call can return a structured diagnostics list on failure.
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
