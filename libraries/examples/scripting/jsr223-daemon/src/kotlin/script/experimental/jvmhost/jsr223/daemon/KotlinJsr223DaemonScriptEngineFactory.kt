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
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.jsr223.base.KotlinJsr223JvmScriptEngineFactoryBase

/**
 * A [KotlinJsr223JvmScriptEngineFactoryBase] that produces [KotlinJsr223DaemonScriptEngineImpl]
 * instances: a JSR-223 engine whose snippet compilation runs out-of-process on the Kotlin compile
 * daemon's regular compile path.
 *
 * Meant to be instantiated directly by embedders that want the daemon-backed engine (for example a
 * host that would rather not bundle the full Kotlin compiler in its own process). It is
 * intentionally not registered as a `javax.script.ScriptEngineFactory` service, so it never
 * competes with the in-process engine for the `"kotlin"`/`"kts"` `ScriptEngineManager`
 * registration.
 *
 * Placed under `libraries/examples/scripting` (rather than `libraries/scripting`) as a portable
 * example an embedder without a Gradle/Build-Tools-API dependency on the project can copy.
 * Everything it needs is the daemon-client API plus a compiler classpath.
 *
 * @param compilerClasspath classpath (jars) the compile daemon is spawned/identified with; must
 *   contain the Kotlin compiler plus the (unshaded) `kotlin-scripting-compiler` plugin jar. See
 *   [DaemonReplCompiler]'s KDoc.
 * @param additionalClasspath extra classpath entries every snippet is compiled against. Most
 *   importantly this should include the Kotlin stdlib, which the daemon compile does not add
 *   implicitly.
 * @param daemonJVMOptions the daemon's JVM options. When `null`, uses the compile daemon-client's
 *   global default; see [DaemonReplCompiler]'s KDoc.
 * @param daemonOptions the daemon's own options (run-files directory, idle-shutdown delay, etc).
 *   When `null`, uses the compile daemon-client's global default.
 * @param daemonLogOptions the daemon's log-file options. When `null`, uses the compile
 *   daemon-client's global default.
 * @param baseCompilationConfiguration the script definition's compilation configuration (for
 *   example `createJvmScriptDefinitionFromTemplate<MainKtsScript>().compilationConfiguration`)
 *   to use for every snippet. The default `ScriptCompilationConfiguration()` keeps the plain,
 *   definition-less behavior. See [KotlinJsr223DaemonScriptEngineImpl]'s KDoc for what this does
 *   and does not affect on this out-of-process compile path.
 * @param baseEvaluationConfiguration the script definition's evaluation configuration (for
 *   example `createJvmScriptDefinitionFromTemplate<MainKtsScript>().evaluationConfiguration`)
 *   used to evaluate every compiled snippet. The default `ScriptEvaluationConfiguration()` keeps
 *   the plain, definition-less behavior.
 */
class KotlinJsr223DaemonScriptEngineFactory(
    private val compilerClasspath: List<File>,
    private val additionalClasspath: List<Path> = emptyList(),
    private val daemonJVMOptions: DaemonJVMOptions? = null,
    private val daemonOptions: DaemonOptions? = null,
    private val daemonLogOptions: DaemonLogOptions? = null,
    private val baseCompilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
    private val baseEvaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration(),
) : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223DaemonScriptEngineImpl(
            this, compilerClasspath, additionalClasspath, daemonJVMOptions, daemonOptions, daemonLogOptions,
            baseCompilationConfiguration, baseEvaluationConfiguration,
        )
}
