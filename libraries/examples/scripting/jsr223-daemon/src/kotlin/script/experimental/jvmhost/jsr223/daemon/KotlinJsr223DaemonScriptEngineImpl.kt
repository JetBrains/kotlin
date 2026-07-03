/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import java.io.File
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
 * A JSR-223 [ScriptEngine] that compiles every snippet **out-of-process**, on the Kotlin compile
 * daemon's regular compile path (see [DaemonReplSnippetCompiler]'s KDoc), instead of embedding the
 * compiler in-process the way
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] does, and without going
 * through the Build Tools API the way
 * [kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineImpl] does.
 *
 * Each [eval] call:
 *  1. sends the snippet source plus every prior snippet's artifact bytes to
 *     [DaemonReplSnippetCompiler.compileSnippet], which runs on the compile daemon;
 *  2. on success, hands the returned artifact to a [DaemonReplSnippetSession] to be materialised
 *     and run in-process, incrementally.
 *
 * One engine instance == one REPL/JSR-223 session: [priorArtifacts] accumulates across calls so
 * later snippets can reference earlier ones' declarations.
 *
 * ### Scope of this first cut
 *
 *  * **No JSR-223 `Bindings`/implicit-receiver support yet.** `$$eval` is invoked with no
 *    arguments -- see the "Bindings / implicit receivers are out of scope" section of
 *    [DaemonReplSnippetSession]'s KDoc.
 *  * Not registered as a `javax.script.ScriptEngineFactory` service -- callers (including this
 *    module's own tests) construct [KotlinJsr223DaemonScriptEngineFactory] directly.
 */
class KotlinJsr223DaemonScriptEngineImpl(
    private val factory: ScriptEngineFactory,
    compilerClasspath: List<File>,
    additionalClasspath: List<Path> = emptyList(),
    daemonRunFilesPath: File = File(System.getProperty("java.io.tmpdir"), "kotlin-jsr223-daemon-run"),
    daemonLogsPath: File = File(System.getProperty("java.io.tmpdir"), "kotlin-jsr223-daemon-logs"),
    shutdownDelayMilliseconds: Long = 0L,
) : AbstractScriptEngine(), ScriptEngine, Compilable {

    private val compiler = DaemonReplSnippetCompiler(
        compilerClasspath, additionalClasspath, daemonRunFilesPath, daemonLogsPath, shutdownDelayMilliseconds
    )

    private var snippetCounter = 0
    private val priorArtifacts = mutableListOf<ByteArray>()
    private val session = DaemonReplSnippetSession()

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = factory

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script)

    override fun eval(reader: Reader, context: ScriptContext): Any? = compileAndEval(reader.readText())

    override fun compile(script: String): CompiledScript = compile(script, context)

    override fun compile(reader: Reader): CompiledScript = compile(reader.readText(), context)

    fun compile(script: String, @Suppress("UNUSED_PARAMETER") context: ScriptContext): CompiledScript =
        KotlinJsr223DaemonCompiledScript(this, compileSnippet(script))

    internal fun compileAndEval(script: String): Any? = runArtifact(compileSnippet(script))

    internal fun runArtifact(artifactBytes: ByteArray): Any? =
        try {
            session.evaluateNext(artifactBytes)
        } catch (e: DaemonReplSnippetEvaluationException) {
            throw ScriptException(e.cause as? Exception ?: RuntimeException(e.cause))
        }

    private fun compileSnippet(script: String): ByteArray {
        val snippetName = "snippet_${snippetCounter++}.repl.kts"
        return when (val result = compiler.compileSnippet(script, snippetName, priorArtifacts.toList())) {
            is DaemonReplSnippetCompilationResult.Success -> result.artifactBytes.also { priorArtifacts += it }
            is DaemonReplSnippetCompilationResult.Failure -> throw ScriptException(
                "Error compiling Kotlin snippet '$snippetName':\n" + result.diagnostics.joinToString("\n")
            )
        }
    }
}

/**
 * A [CompiledScript] produced by [KotlinJsr223DaemonScriptEngineImpl.compile]: the snippet has
 * already been compiled (and its artifact recorded into the engine's history for subsequent
 * compiles), but not yet run -- [eval] runs it.
 */
class KotlinJsr223DaemonCompiledScript internal constructor(
    private val engine: KotlinJsr223DaemonScriptEngineImpl,
    private val artifactBytes: ByteArray,
) : CompiledScript() {
    override fun eval(context: ScriptContext): Any? = engine.runArtifact(artifactBytes)
    override fun getEngine(): ScriptEngine = engine
}
