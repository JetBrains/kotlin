/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.daemonExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.compileReplSnippetOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.ReplSnippetCompilationResult
import java.io.Reader
import java.nio.file.Path
import javax.script.AbstractScriptEngine
import javax.script.Bindings
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import javax.script.ScriptException
import javax.script.SimpleBindings

/**
 * A JSR-223 [ScriptEngine] that compiles every snippet **out-of-process**, through the Kotlin
 * Build Tools API's `CompileReplSnippetOperation` (see
 * `plugins/scripting/.ai/target/40-jsr223-target.md` "Remote (out-of-process) compilation"),
 * instead of embedding the compiler in-process the way
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] does.
 *
 * Each [eval] call:
 *  1. sends the snippet source plus every prior snippet's artifact bytes to a
 *     [CompileReplSnippetOperation], executed with [ExecutionPolicy.WithDaemon] (the only policy
 *     this op supports — it rejects [ExecutionPolicy.InProcess] by design);
 *  2. on [ReplSnippetCompilationResult.Success], hands the returned artifact to a
 *     [BtaReplSnippetSession] to be materialised and run in-process, incrementally.
 *
 * One engine instance == one REPL/JSR-223 session: [priorArtifacts] accumulates across calls so
 * later snippets can reference earlier ones' declarations, exactly like the in-process engine's
 * per-`ScriptEngine` `K2ReplCompiler`/`K2ReplEvaluator` state.
 *
 * ### Scope of this first cut
 *
 *  * **No JSR-223 `Bindings`/implicit-receiver support yet.** `$$eval` is invoked with no
 *    arguments — see the "Bindings / implicit receivers are out of scope" section of
 *    [BtaReplSnippetSession]'s KDoc for why, and what would be needed to add it.
 *  * Not registered as a `javax.script.ScriptEngineFactory` service — see
 *    [KotlinJsr223BtaScriptEngineFactory]'s KDoc.
 */
@ExperimentalBuildToolsApi
class KotlinJsr223BtaScriptEngineImpl(
    private val factory: ScriptEngineFactory,
    implementationClasspath: List<Path>,
    private val additionalClasspath: List<Path> = emptyList(),
    daemonPolicyConfiguration: ExecutionPolicy.WithDaemon.Builder.() -> Unit = {},
) : AbstractScriptEngine(), ScriptEngine, Compilable {

    private val toolchains: KotlinToolchains = KotlinToolchains.loadImplementation(implementationClasspath)
    private val buildSession: KotlinToolchains.BuildSession = toolchains.createBuildSession()
    private val daemonPolicy: ExecutionPolicy.WithDaemon = toolchains.daemonExecutionPolicy(daemonPolicyConfiguration)

    private var snippetCounter = 0
    private val priorArtifacts = mutableListOf<ByteArray>()
    private val session = BtaReplSnippetSession()

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = factory

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script)

    override fun eval(reader: Reader, context: ScriptContext): Any? = compileAndEval(reader.readText())

    override fun compile(script: String): CompiledScript = compile(script, context)

    override fun compile(reader: Reader): CompiledScript = compile(reader.readText(), context)

    fun compile(script: String, @Suppress("UNUSED_PARAMETER") context: ScriptContext): CompiledScript =
        KotlinJsr223BtaCompiledScript(this, compileSnippet(script))

    internal fun compileAndEval(script: String): Any? = runArtifact(compileSnippet(script))

    internal fun runArtifact(artifactBytes: ByteArray): Any? =
        try {
            session.evaluateNext(artifactBytes)
        } catch (e: BtaReplSnippetEvaluationException) {
            throw ScriptException(e.cause as? Exception ?: RuntimeException(e.cause))
        }

    private fun compileSnippet(script: String): ByteArray {
        val snippetName = "snippet_${snippetCounter++}.repl.kts"
        val operation = toolchains.jvm.compileReplSnippetOperation(priorArtifacts.toList(), script, snippetName) {
            if (additionalClasspath.isNotEmpty()) {
                this[CompileReplSnippetOperation.ADDITIONAL_CLASSPATH] = additionalClasspath
            }
        }
        return when (val result = buildSession.executeOperation(operation, daemonPolicy)) {
            is ReplSnippetCompilationResult.Success -> result.artifact.also { priorArtifacts += it }
            is ReplSnippetCompilationResult.Failure -> throw ScriptException(
                "Error compiling Kotlin snippet '$snippetName':\n" +
                        result.diagnostics.joinToString("\n") { "${it.severity}: ${it.message}" }
            )
        }
    }

    /**
     * Releases the underlying [KotlinToolchains.BuildSession] (and, transitively, lets the Kotlin
     * daemon shut down once its idle-shutdown delay elapses). Call once this engine is no longer
     * needed.
     */
    fun close() {
        buildSession.close()
    }
}

/**
 * A [CompiledScript] produced by [KotlinJsr223BtaScriptEngineImpl.compile]: the snippet has already
 * been compiled (and its artifact recorded into the engine's history for subsequent compiles), but
 * not yet run — [eval] runs it, exactly once being idiomatic for `Compilable`/`CompiledScript`
 * usage, though nothing prevents calling it more than once (each call re-invokes `$$eval`).
 */
@ExperimentalBuildToolsApi
class KotlinJsr223BtaCompiledScript internal constructor(
    private val engine: KotlinJsr223BtaScriptEngineImpl,
    private val artifactBytes: ByteArray,
) : CompiledScript() {
    override fun eval(context: ScriptContext): Any? = engine.runArtifact(artifactBytes)
    override fun getEngine(): ScriptEngine = engine
}
