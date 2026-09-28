/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.bta

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
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
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplSnippetConfigurationCodec
import java.io.IOException
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
 * A [ReplCompiler] that compiles snippets out-of-process through the Build Tools API's ordinary JVM
 * compilation operation. Each snippet is a plain `.repl.<extension>` source file, and the compilation
 * is switched into chained-snippet mode by the scripting plugin's `repl-snippet-*` options
 * (see `ScriptingCommandLineProcessor`). Earlier snippets are fed back as classpath entries plus their [ClassId]s.
 *
 * @param compilerClasspath the Build Tools API implementation jar plus the compiler it runs on.
 * @param scriptingPluginClasspath the scripting compiler plugin jar; must match the compiler in
 *   [compilerClasspath] - with the embeddable implementation.
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

    private val workRoot: Path = Files.createTempDirectory("jsr223-bta-repl-work-")
    private var snippetCounter = 0

    private val logger = CollectingKotlinLogger()

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
    private fun compileSnippetBatch(
        batch: List<Pair<SourceCode, ScriptCompilationConfiguration>>,
        reports: MutableList<ScriptDiagnostic>,
    ): ResultWithDiagnostics<Unit> {
        data class RefinedSnippet(val snippet: SourceCode, val name: String, val configuration: ScriptCompilationConfiguration)

        // The beforeCompiling handlers have to be run here, there being no local FIR session to run
        // them implicitly. Their effect reaches the other process through the transported
        // configuration (see scriptingPlugin), which is what makes e.g. the bindings receiver appear
        // as a receiver parameter of the snippet classes.
        val refinedSnippets = batch.map { [snippet, snippetConfiguration] ->
            val snippetName = snippet.name
                ?: return ResultWithDiagnostics.Failure("BtaReplCompiler: snippet has no name".asErrorDiagnostics())
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
        val outputDir = Files.createDirectories(workRoot.resolve("snippet-batch-$batchIndex-out"))
        val configurationFile = workRoot.resolve("snippet-batch-$batchIndex-configuration.bin").also {
            ReplSnippetConfigurationCodec.writeTo(batchConfiguration, it.toFile())
        }
        val sourceDir = Files.createTempDirectory("jsr223-bta-repl-snippet-src-")
        try {
            val scriptFiles = refinedSnippets.map { sourceDir.resolve(it.name).also { file -> file.writeText(it.snippet.text) } }

            logger.clear()
            val result = runCompilation(scriptFiles, outputDir, configurationFile)
            if (result != COMPILATION_SUCCESS) {
                return ResultWithDiagnostics.Failure(
                    logger.messages.map { it.asErrorDiagnostics(path = refinedSnippets.last().snippet.locationId) }
                )
            }

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
            sourceDir.deleteRecursivelyWithRetries()
        }
    }

    @OptIn(ExperimentalCompilerArgument::class, DeprecatedCompilerArgument::class)
    private fun runCompilation(
        scriptFiles: List<Path>,
        outputDir: Path,
        configurationFile: Path,
    ): CompilationResult {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(scriptFiles, outputDir)
        operation.compilerArguments.let { arguments ->
            arguments[JvmCompilerArguments.CLASSPATH] = additionalClasspath + priorOutputDirs
            arguments[CommonCompilerArguments.X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS] = true
            arguments[CommonCompilerArguments.X_USE_FIR_LT] = false // TODO: remove after finishing KT-77583
            arguments[CommonCompilerArguments.X_SUPPRESS_VERSION_WARNINGS] = true
            arguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(scriptingPlugin(configurationFile))
        }
        return session.executeOperation(operation.build(), executionPolicy, logger)
    }

    /**
     * The scripting compiler plugin declaration that switches this compilation into
     * chained-REPL-snippet mode.
     */
    private fun scriptingPlugin(configurationFile: Path): CompilerPlugin =
        CompilerPlugin(
            pluginId = KOTLIN_SCRIPTING_PLUGIN_ID,
            classpath = scriptingPluginClasspath,
            rawArguments = buildList {
                add(CompilerPluginOption("repl-snippet-stateless-mode", "true"))
                priorClassIds.lastOrNull()?.let { classId ->
                    add(CompilerPluginOption("repl-snippet-prior-class", classId.asString()))
                }
                add(CompilerPluginOption("repl-snippet-configuration", configurationFile.toAbsolutePath().toString()))
            },
            orderingRequirements = emptySet(),
        )

    // The compile daemon is left to its own idle-shutdown settings, since it may be shared with
    // other clients.
    override fun close() {
        if (sessionLazy.isInitialized()) {
            session.close()
        }
        workRoot.deleteRecursivelyWithRetries()
    }

    private fun snippetClassId(scriptFile: Path): ClassId =
        ClassId(ROOT, NameUtils.getSnippetTargetClassName(scriptFile.name))
}

// defensive deleting for OSes like Windows
@OptIn(ExperimentalPathApi::class)
internal fun Path.deleteRecursivelyWithRetries(timeoutMillis: Long = 10_000): Boolean {
    val deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000
    while (true) {
        try {
            deleteRecursively()
            return true
        } catch (_: IOException) {
            if (System.nanoTime() >= deadlineNanos) return false
            Thread.sleep(100)
        }
    }
}

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
