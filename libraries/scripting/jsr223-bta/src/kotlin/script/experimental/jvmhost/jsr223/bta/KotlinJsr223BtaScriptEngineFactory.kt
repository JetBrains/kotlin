/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import java.nio.file.Path
import javax.script.ScriptEngine
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineFactoryBase

/**
 * A [KotlinJsr223JvmScriptEngineFactoryBase] that produces [KotlinJsr223BtaScriptEngineImpl]
 * instances -- a JSR-223 engine whose snippet compilation runs **out-of-process**, through the
 * Kotlin Build Tools API's `CompileReplSnippetOperation`, rather than being driven by an in-process
 * K2 REPL compiler the way
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223DefaultScriptEngineFactory] is.
 *
 * This factory is meant to be **instantiated directly by embedders** that want the BTA-backed
 * engine -- e.g. a host that would rather not bundle the full Kotlin compiler in its own process
 * (see `plugins/scripting/.ai/target/40-jsr223-target.md` "Remote (out-of-process) compilation").
 * It is intentionally **not** registered as a `javax.script.ScriptEngineFactory` service
 * (there is no `META-INF/services/javax.script.ScriptEngineFactory` entry for it in this module),
 * so it never competes with the in-process engine for the `"kotlin"`/`"kts"` `ScriptEngineManager`
 * registration -- callers construct it explicitly (as the tests in this module do) rather than
 * looking it up via `javax.script`.
 *
 * @param implementationClasspath classpath (jars) for a Build Tools API implementation --
 *   typically `kotlin-build-tools-impl` plus the Kotlin compiler and its dependencies. Passed
 *   directly to [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.loadImplementation]; per that
 *   method's contract, this must resolve to a [java.net.URLClassLoader] to support
 *   [ExecutionPolicy.WithDaemon].
 * @param additionalClasspath extra classpath entries every snippet is compiled against -- most
 *   importantly the Kotlin stdlib, which [org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation]
 *   does not add implicitly (mirrors its `ADDITIONAL_CLASSPATH` option).
 * @param daemonPolicyConfiguration optional customization of the [ExecutionPolicy.WithDaemon] used
 *   for every compile (daemon run-dir/log paths, JVM args, shutdown delay, etc.).
 */
@ExperimentalBuildToolsApi
class KotlinJsr223BtaScriptEngineFactory(
    private val implementationClasspath: List<Path>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonPolicyConfiguration: ExecutionPolicy.WithDaemon.Builder.() -> Unit = {},
) : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223BtaScriptEngineImpl(this, implementationClasspath, additionalClasspath, daemonPolicyConfiguration)
}
