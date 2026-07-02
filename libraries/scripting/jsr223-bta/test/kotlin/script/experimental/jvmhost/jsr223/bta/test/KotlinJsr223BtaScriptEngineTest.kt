/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta.test

import kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineFactory
import kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineImpl
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Exercises [KotlinJsr223BtaScriptEngineImpl] end-to-end, against a **real Kotlin compile daemon**
 * (not an in-process/faked transport) -- mirroring how `ReplSnippetCompilationTest`
 * (`:kotlin-build-tools-api-tests`) exercises the underlying `CompileReplSnippetOperation`.
 *
 * The engine is **manually instantiated** via [KotlinJsr223BtaScriptEngineFactory], not looked up
 * through `javax.script.ScriptEngineManager`: [KotlinJsr223BtaScriptEngineFactory] is deliberately
 * not registered as a `javax.script.ScriptEngineFactory` service (see its KDoc), so a real
 * `ScriptEngineManager` lookup would never find it. Direct instantiation is the intended usage for
 * now, until a dedicated module wires this engine into a real JSR-223 service registration (the
 * way `:kotlin-scripting-jsr223-test` does for the in-process engine).
 */
@OptIn(ExperimentalBuildToolsApi::class)
class KotlinJsr223BtaScriptEngineTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val implementationClasspath: List<Path> = classpathFromSystemProperty("kotlinBtaImplClasspath")

    // The compiler needs the stdlib on the snippet's compile classpath (it is not added
    // implicitly by CompileReplSnippetOperation). Resolved through a classloader built the same
    // way `KotlinToolchains.loadImplementation(List<Path>)` builds its own internally, so the
    // stdlib version always matches the one the loaded implementation actually uses -- exactly the
    // technique `currentKotlinStdlibLocation` (`:kotlin-build-tools-api-tests`) uses.
    private val stdlib: Path by lazy {
        val btaClassloader = URLClassLoader(
            implementationClasspath.map { it.toUri().toURL() }.toTypedArray(),
            SharedApiClassesClassLoader(),
        )
        Paths.get(btaClassloader.loadClass(KotlinVersion::class.qualifiedName).protectionDomain.codeSource.location.toURI())
    }

    private var engineToClose: KotlinJsr223BtaScriptEngineImpl? = null

    private fun newEngine(): KotlinJsr223BtaScriptEngineImpl {
        val factory = KotlinJsr223BtaScriptEngineFactory(
            implementationClasspath = implementationClasspath,
            additionalClasspath = listOf(stdlib),
            daemonPolicyConfiguration = {
                @OptIn(DelicateBuildToolsApi::class)
                this[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH] = daemonRunDir
                this[ExecutionPolicy.WithDaemon.LOGS_PATH] = daemonRunDir
                this[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 0
            },
        )
        return (factory.scriptEngine as KotlinJsr223BtaScriptEngineImpl).also { engineToClose = it }
    }

    @AfterEach
    fun closeEngine() {
        engineToClose?.close()
        engineToClose = null
    }

    @Test
    fun testSimpleEval() {
        val engine = newEngine()
        assertEquals(42, engine.eval("40 + 2"))
    }

    @Test
    fun testCrossSnippetReference() {
        val engine = newEngine()
        engine.eval("val x = 10")
        assertEquals(20, engine.eval("x * 2"))
    }

    @Test
    fun testCompileThenEvalSeparately() {
        val engine = newEngine()
        val compiled = engine.compile("6 * 7")
        assertEquals(42, compiled.eval())
    }

    @Test
    fun testDeclarationOnlySnippetEvaluatesToNull() {
        val engine = newEngine()
        assertEquals(null, engine.eval("val onlyADeclaration = 1"))
    }
}

private fun classpathFromSystemProperty(propertyName: String): List<Path> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { Paths.get(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")
