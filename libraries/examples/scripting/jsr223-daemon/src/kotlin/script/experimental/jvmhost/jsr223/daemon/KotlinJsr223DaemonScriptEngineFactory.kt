/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import java.io.File
import java.nio.file.Path
import javax.script.ScriptEngine
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineFactoryBase

/**
 * A [KotlinJsr223JvmScriptEngineFactoryBase] that produces [KotlinJsr223DaemonScriptEngineImpl]
 * instances -- a JSR-223 engine whose snippet compilation runs **out-of-process**, on the Kotlin
 * compile daemon's regular compile path, rather than being driven by an in-process K2 REPL
 * compiler (as
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223DefaultScriptEngineFactory] is).
 *
 * This factory is meant to be **instantiated directly by embedders** that want the daemon-backed
 * engine -- e.g. a host that would rather not bundle the full Kotlin compiler in its own process
 * (see `plugins/scripting/.ai/target/40-jsr223-target.md` "Remote (out-of-process) compilation").
 * It is intentionally **not** registered as a `javax.script.ScriptEngineFactory` service, so it
 * never competes with the in-process engine for the `"kotlin"`/`"kts"` `ScriptEngineManager`
 * registration -- callers construct it explicitly (as the tests in this module do) rather than
 * looking it up via `javax.script`.
 *
 * This module is placed under `libraries/examples/scripting` (rather than
 * `libraries/scripting`) because it is meant as a **portable example** an embedder without a
 * Gradle/Build-Tools-API dependency on the project (e.g. IntelliJ) can copy wholesale: everything
 * it needs is the daemon-client API plus a compiler classpath.
 *
 * @param compilerClasspath classpath (jars) the compile daemon is spawned/identified with; must
 *   contain the Kotlin compiler plus the (unshaded) `kotlin-scripting-compiler` plugin jar -- see
 *   [DaemonReplCompiler]'s KDoc.
 * @param additionalClasspath extra classpath entries every snippet is compiled against -- most
 *   importantly the Kotlin stdlib, which the daemon compile does not add implicitly.
 * @param daemonJVMOptions the daemon's JVM options; `null` (the default) uses the compile
 *   daemon-client's own global default -- see [DaemonReplCompiler]'s KDoc.
 * @param daemonOptions the daemon's own options (run-files directory, idle-shutdown delay, etc);
 *   `null` (the default) uses the compile daemon-client's own global default.
 * @param daemonLogOptions the daemon's log-file options; `null` (the default) uses the compile
 *   daemon-client's own global default.
 */
class KotlinJsr223DaemonScriptEngineFactory(
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJVMOptions: DaemonJVMOptions? = null,
    private val daemonOptions: DaemonOptions? = null,
    private val daemonLogOptions: DaemonLogOptions? = null,
) : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223DaemonScriptEngineImpl(
            this, compilerClasspath, additionalClasspath, daemonJVMOptions, daemonOptions, daemonLogOptions
        )
}
