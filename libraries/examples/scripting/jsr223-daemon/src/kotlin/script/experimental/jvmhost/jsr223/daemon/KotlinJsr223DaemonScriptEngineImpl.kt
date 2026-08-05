/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import java.io.File
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.jsr223.base.KotlinJsr223JvmScriptEngineBase

/**
 * Per-JSR-223-session state, mirroring
 * [kotlin.script.experimental.jvmhost.jsr223.K2ReplState] -- the only difference is [compiler]'s
 * type: a [DaemonReplCompiler] rather than an in-process
 * [org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler].
 */
data class DaemonReplState(
    val compiler: DaemonReplCompiler,
    val evaluator: K2ReplEvaluator,
    var snippetCounter: Int = 0,
)

/**
 * A JSR-223 [ScriptEngine] that compiles every snippet **out-of-process**, on the Kotlin compile
 * daemon's regular compile path (see [DaemonReplCompiler]'s KDoc), instead of embedding the
 * compiler in-process the way
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] does.
 *
 * Unlike an earlier iteration of this module (which drove a bespoke, reflection-based
 * `DaemonReplSnippetSession` and a from-scratch `DaemonReplSnippetCompiler`-only compile loop),
 * this class is built directly on top of the **stock** REPL infrastructure that
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl] itself uses:
 *  * [KotlinJsr223JvmScriptEngineBase] supplies the JSR-223 state/`Bindings` plumbing, the live
 *    `ScriptContext` tracking and the whole compile/eval loop, exactly as it does for the in-process
 *    engine.
 *  * [K2ReplEvaluator] runs every compiled snippet completely unmodified -- including its
 *    existing cross-snippet classloader chaining (`ScriptEvaluationConfiguration.jvm.lastSnippetClassLoader`),
 *    which resolves references to earlier snippets' declarations without any extra code here.
 *
 * The **only** substitution is [DaemonReplCompiler] in place of `K2ReplCompiler` -- see its KDoc
 * for how the daemon's compiled output is turned into a real
 * [kotlin.script.experimental.jvm.impl.KJvmCompiledScript] that the stock evaluator can consume.
 *
 * ### Scope of this first cut
 *
 *  * **No `Invocable` support.** `invokeFunction`/`invokeMethod`/`getInterface` are unimplemented
 *    (this engine does not implement
 *    [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223InvocableScriptEngine]) -- see the
 *    "Bindings" section below for what *is* supported.
 *  * Not registered as a `javax.script.ScriptEngineFactory` service -- callers (including this
 *    module's own tests) construct [KotlinJsr223DaemonScriptEngineFactory] directly.
 *
 * ### Supplying a custom script definition
 *
 * [baseCompilationConfiguration]/[baseEvaluationConfiguration] let a caller plug in a script
 * definition's own configurations -- e.g.
 * `createJvmScriptDefinitionFromTemplate<org.jetbrains.kotlin.mainKts.MainKtsScript>()`'s -- instead
 * of the plain, definition-less defaults. Every snippet still physically compiles as a **plain,
 * unmarked REPL snippet** on the daemon's regular compile path (see [DaemonReplCompiler]'s KDoc) --
 * named `snippet_N.repl.<extension>` (see [KotlinJsr223JvmScriptEngineBase]), where `<extension>` is
 * the configuration's own [ScriptCompilationConfiguration.fileExtension] (e.g. `main.kts` for
 * `MainKtsScript`), so [DaemonReplCompiler] recognizes it as such even for a non-default definition
 * -- but the compilation configuration is *not* otherwise fed through the daemon compile itself --
 * its `refineConfiguration` hooks (annotation handlers such as `MainKtsScriptDefinition`'s
 * `@file:Import`/`@file:DependsOn` resolver, `beforeCompiling` callbacks, etc.) never run, since
 * there is no in-process refinement step on this path at all. What it *does* do:
 * [DaemonReplCompiler] threads it straight into every compiled snippet's own
 * [kotlin.script.experimental.jvm.impl.KJvmCompiledScript.compilationConfiguration], which
 * [K2ReplEvaluator] reads back at evaluation time (`compiledSnippet.compilationConfiguration`) and
 * merges into the evaluation configuration before running that snippet's own
 * `refineConfigurationBeforeEvaluate` hooks -- e.g. `MainKtsEvaluationConfiguration`'s
 * `configureConstructorArgsFromMainArgs`. In short: a script definition's *compile-time* dependency
 * resolution/import machinery is out of scope here, but its *evaluation-time* configuration
 * (`constructorArgs`, `implicitReceivers`, `providedProperties`, ...) is fully honored.
 *
 * ### Bindings support
 *
 * The bindings-exposure machinery is inherited wholesale from [KotlinJsr223JvmScriptEngineBase]: a
 * [baseCompilationConfiguration] which reuses
 * [kotlin.script.experimental.jvm.jsr223.configureExposedJsr223Context]/
 * [kotlin.script.experimental.jvm.jsr223.generateBindingSnippetIfNeeded] (e.g.
 * `KotlinJsr223DefaultScriptCompilationConfiguration`, `MainKtsScript`) generates its
 * bindings-exposing synthetic snippet -- and its implicit-receiver additions
 * (`ScriptContext`/`ScriptTemplateWithBindings`) -- client-side, exactly as the in-process engine
 * does. Unlike the in-process engine, though, there is no local FIR session here to run
 * `refineConfiguration`'s `beforeCompiling` hooks implicitly, so [DaemonReplCompiler.compile]
 * explicitly runs them itself (`refineBeforeCompiling`) and additionally has to smuggle the
 * resulting implicit-receiver *type names* across the client/daemon process boundary through a
 * dedicated CLI option, since a snippet's *source* referring to such a receiver unqualified (e.g.
 * `getBindings(...)`) would otherwise fail to even compile on the daemon -- see
 * [DaemonReplCompiler.compile]'s KDoc for the full mechanism and why this is the one piece of a
 * script definition's `ScriptCompilationConfiguration` that genuinely has to cross that boundary.
 * End to end, a value read from or put into `getContext()`'s (or an explicitly passed
 * `ScriptContext`'s) `Bindings` is visible to (and can be written back from) a snippet as an
 * ordinary property, the same as with the in-process engine.
 *
 * Call [close] once this engine is no longer needed -- see [DaemonReplCompiler]'s KDoc for why the
 * underlying compile-daemon connection must be disposed explicitly rather than per-compile.
 */
class KotlinJsr223DaemonScriptEngineImpl(
    factory: ScriptEngineFactory,
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJVMOptions: DaemonJVMOptions? = null,
    private val daemonOptions: DaemonOptions? = null,
    private val daemonLogOptions: DaemonLogOptions? = null,
    baseCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
    baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
) : KotlinJsr223JvmScriptEngineBase<DaemonReplState>(factory) {

    override var compilationConfiguration: ScriptCompilationConfiguration =
        ScriptCompilationConfiguration(baseCompilationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    override val evaluationConfiguration: ScriptEvaluationConfiguration =
        ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
            hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
        }

    override val replCompiler: DaemonReplCompiler get() = getCurrentState(getContext()).compiler
    override val replEvaluator: K2ReplEvaluator get() = getCurrentState(getContext()).evaluator

    override fun createState(lock: ReentrantReadWriteLock): DaemonReplState =
        DaemonReplState(
            DaemonReplCompiler(
                compilerClasspath, additionalClasspath,
                daemonJVMOptions = daemonJVMOptions, daemonOptions = daemonOptions, daemonLogOptions = daemonLogOptions,
            ),
            K2ReplEvaluator(),
        )

    override fun nextSnippetNo(): Int = getCurrentState(getContext()).snippetCounter++

    /**
     * Releases the underlying [DaemonReplCompiler]'s compile-daemon session (see its KDoc). Call
     * once this engine is no longer needed.
     */
    fun close() {
        replCompiler.close()
    }

    /**
     * Test-only hook: see [DaemonReplCompiler.forceShutdownDaemon]'s KDoc.
     */
    fun forceShutdownDaemonForTests() {
        replCompiler.forceShutdownDaemon()
    }
}
