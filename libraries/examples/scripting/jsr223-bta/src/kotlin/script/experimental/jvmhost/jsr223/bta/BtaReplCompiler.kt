/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.script.experimental.api.*
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.jvm.impl.compiledSnippetFromClassPath
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add

/**
 * A [ReplCompiler] that compiles snippets out-of-process through the Build Tools API's regular JVM
 * compilation operation: there is no REPL-specific operation or artifact protocol. Each snippet is a
 * plain `.repl.<extension>` source file, and the compilation is switched into chained-snippet mode
 * by the scripting plugin's `repl-snippet-*` options (see `ScriptingCommandLineProcessor`). Earlier
 * snippets are fed back purely as classpath entries plus their [ClassId]s.
 *
 * A compile failure is reported as a [ResultWithDiagnostics.Failure]; exceptions are reserved for
 * precondition and infrastructure errors.
 *
 * @param compilerClasspath the Build Tools API implementation jar plus the compiler it runs on.
 * @param scriptingPluginClasspath the scripting compiler plugin jar; must match the compiler in
 *   [compilerClasspath] - with the embeddable implementation, the embeddable, non-relocated one.
 * @param additionalClasspath entries every snippet is compiled against, notably the Kotlin stdlib,
 *   which is not added implicitly.
 */
class BtaReplCompiler(
    private val compilerClasspath: List<Path>,
    private val scriptingPluginClasspath: List<Path>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJvmArguments: List<String>? = null,
    private val daemonRunFilesPath: Path? = null,
    private val daemonLogsPath: Path? = null,
    private val daemonShutdownDelayMillis: Long? = null,
) : ReplCompiler<CompiledSnippet>, AutoCloseable {

    // Snippet output directories must outlive their compile call: K2ReplEvaluator's classloader
    // chain keeps referencing them. Deleted in close().
    private val workRoot: Path = Files.createTempDirectory("jsr223-bta-repl-work-")
    private var snippetCounter = 0

    private val logger = CollectingKotlinLogger()

    // Loading the implementation spins up an isolated classloader holding the whole compiler, so the
    // toolchain and the session below are created once and kept for this compiler's lifetime.
    private val toolchain: KotlinToolchains by lazy { KotlinToolchains.loadImplementation(compilerClasspath) }

    @OptIn(DelicateBuildToolsApi::class)
    private val executionPolicy: ExecutionPolicy by lazy {
        toolchain.daemonExecutionPolicyBuilder().apply {
            if (daemonJvmArguments != null) set(ExecutionPolicy.WithDaemon.JVM_ARGUMENTS, daemonJvmArguments)
            if (daemonRunFilesPath != null) set(ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH, daemonRunFilesPath)
            if (daemonLogsPath != null) set(ExecutionPolicy.WithDaemon.LOGS_PATH, daemonLogsPath)
            if (daemonShutdownDelayMillis != null) {
                set(ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS, daemonShutdownDelayMillis)
            }
        }.build()
    }

    private val sessionLazy: Lazy<KotlinToolchains.BuildSession> = lazy { toolchain.createBuildSession() }
    private val session: KotlinToolchains.BuildSession by sessionLazy

    // Fed back to the compiler as classpath entries plus `repl-snippet-prior-class`es, so that it can
    // resolve cross-snippet references.
    private val priorOutputDirs = mutableListOf<Path>()
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
     * Compiles synthetic snippets together with the main snippet they were generated for, as several
     * sources of a single compilation. They cannot be split into separate compilations: an earlier
     * sibling has no bytecode yet (codegen runs once, after all bodies are resolved), so it is
     * resolved through the compiler's live, same-session snippet tracking rather than the classpath.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun compileSnippetBatch(
        batch: List<Pair<SourceCode, ScriptCompilationConfiguration>>,
        reports: MutableList<ScriptDiagnostic>,
    ): ResultWithDiagnostics<Unit> {
        data class RefinedSnippet(val snippet: SourceCode, val name: String, val configuration: ScriptCompilationConfiguration)

        // The beforeCompiling handlers have to be run here, there being no local FIR session to run
        // them implicitly. Only their implicitReceivers effect matters on this path, and it has to
        // reach the other process (see scriptingPlugin) for the snippet classes to declare matching
        // receiver parameters.
        val refinedSnippets = batch.map { [snippet, snippetConfiguration] ->
            val snippetName = snippet.name
                ?: return ResultWithDiagnostics.Failure("BtaReplCompiler: snippet has no name".asErrorDiagnostics())
            val refinedConfiguration = snippetConfiguration.refineBeforeCompiling(snippet).valueOr { return it }
            RefinedSnippet(snippet, snippetName, refinedConfiguration)
        }
        val implicitReceiverTypeNames = refinedSnippets
            .flatMap { it.configuration[ScriptCompilationConfiguration.implicitReceivers].orEmpty().map { r -> r.typeName } }
            .distinct()

        // Shared by the whole batch, since it comes from the single base configuration.
        val fileExtension = refinedSnippets.first().configuration[ScriptCompilationConfiguration.fileExtension] ?: "kts"

        val outputDir = Files.createDirectories(workRoot.resolve("snippet-batch-${snippetCounter++}-out"))
        val sourceDir = Files.createTempDirectory("jsr223-bta-repl-snippet-src-")
        try {
            val scriptFiles = refinedSnippets.map { sourceDir.resolve(it.name).also { file -> file.writeText(it.snippet.text) } }

            logger.clear()
            val result = runCompilation(scriptFiles, outputDir, implicitReceiverTypeNames, fileExtension)
            if (result != CompilationResult.COMPILATION_SUCCESS) {
                return ResultWithDiagnostics.Failure(
                    logger.messages.map { it.asErrorDiagnostics(path = refinedSnippets.last().snippet.locationId) }
                )
            }

            // Not `+=`: a Path is an Iterable<Path>, so that would resolve to the collection overload.
            priorOutputDirs.add(outputDir)
            for ([refinedSnippet, scriptFile] in refinedSnippets.zip(scriptFiles)) {
                val classId = snippetClassId(scriptFile)
                val compiledSnippet = compiledSnippetFromClassPath(
                    classPath = listOf(outputDir.toFile()),
                    snippetClassFQName = classId.asSingleFqName().asString(),
                    snippet = refinedSnippet.snippet,
                    compilationConfiguration = refinedSnippet.configuration,
                )
                priorClassIds += classId
                lastCompiledSnippetInternal = lastCompiledSnippetInternal.add(compiledSnippet)
            }
            reports += logger.messages.map {
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
     * Compiles [scriptFiles] as chained REPL snippets within one regular JVM compilation operation.
     *
     * The snippets are ordinary source files rather than an evaluated expression or script: the
     * latter route through `ScriptEvaluationExtension.eval()`, which would risk running a throwing
     * snippet inside the compiler's process. Their `.repl.<extension>` names are what make
     * `ScriptingProcessSourcesBeforeCompilingExtension` treat them as snippets.
     *
     * [CommonCompilerArguments.X_USE_FIR_LT] must stay off: that extension only runs on the
     * PSI-based source-collection path, and in light-tree mode (the JVM pipeline's default) a
     * snippet would silently compile as a plain script instead. The deprecated flag makes this the
     * part of the design most likely to need revisiting.
     */
    @OptIn(ExperimentalCompilerArgument::class, org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument::class)
    private fun runCompilation(
        scriptFiles: List<Path>,
        outputDir: Path,
        implicitReceiverTypeNames: List<String>,
        fileExtension: String,
    ): CompilationResult {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(scriptFiles, outputDir)
        operation.compilerArguments.let { arguments ->
            arguments[JvmCompilerArguments.CLASSPATH] = additionalClasspath + priorOutputDirs
            arguments[CommonCompilerArguments.X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS] = true
            arguments[CommonCompilerArguments.X_USE_FIR_LT] = false
            arguments[CommonCompilerArguments.X_SUPPRESS_VERSION_WARNINGS] = true
            arguments[CommonCompilerArguments.COMPILER_PLUGINS] =
                listOf(scriptingPlugin(implicitReceiverTypeNames, fileExtension))
        }
        return session.executeOperation(operation.build(), executionPolicy, logger)
    }

    /**
     * The scripting compiler plugin declaration that switches this compilation into
     * chained-REPL-snippet mode.
     */
    private fun scriptingPlugin(implicitReceiverTypeNames: List<String>, fileExtension: String): CompilerPlugin =
        CompilerPlugin(
            pluginId = KOTLIN_SCRIPTING_PLUGIN_ID,
            classpath = scriptingPluginClasspath,
            rawArguments = buildList {
                add(CompilerPluginOption("repl-snippet-regular-mode", "true"))
                for (classId in priorClassIds) {
                    add(CompilerPluginOption("repl-snippet-prior-class", classId.asString()))
                }
                for (implicitReceiverTypeName in implicitReceiverTypeNames) {
                    add(CompilerPluginOption("repl-snippet-implicit-receiver", implicitReceiverTypeName))
                }
                add(CompilerPluginOption("repl-snippet-file-extension", fileExtension))
            },
            orderingRequirements = emptySet(),
        )

    // The compile daemon is left to its own idle-shutdown settings, since it may be shared with
    // other clients.
    @OptIn(ExperimentalPathApi::class)
    override fun close() {
        if (sessionLazy.isInitialized()) {
            session.close()
        }
        workRoot.deleteRecursively()
    }

    /**
     * Predicts the [ClassId] a snippet compiles to, with no round-trip to the compiler: the FIR
     * snippet builder derives it from the source file name alone, in the root package.
     */
    private fun snippetClassId(scriptFile: Path): ClassId =
        ClassId(FqName.ROOT, NameUtils.getSnippetTargetClassName(scriptFile.name))
}

/**
 * Captures compiler messages so that they can be turned into script diagnostics. The levels below
 * warning carry the Build Tools API's own progress reporting, not snippet diagnostics, and are
 * dropped.
 */
private class CollectingKotlinLogger : KotlinLogger {
    private val collected = mutableListOf<String>()

    val messages: List<String> get() = collected

    override val isDebugEnabled: Boolean get() = false

    fun clear() {
        collected.clear()
    }

    override fun error(msg: String, throwable: Throwable?) {
        collected.add("error: $msg${throwable.suffix()}")
    }

    override fun warn(msg: String, throwable: Throwable?) {
        collected.add("warning: $msg${throwable.suffix()}")
    }

    override fun info(msg: String) {}

    override fun lifecycle(msg: String) {}

    override fun debug(msg: String) {}

    private fun Throwable?.suffix(): String = this?.let { " (${it.message})" } ?: ""
}
