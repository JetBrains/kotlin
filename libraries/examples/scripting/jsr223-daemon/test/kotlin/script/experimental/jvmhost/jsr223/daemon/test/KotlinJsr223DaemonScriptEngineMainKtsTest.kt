/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon.test

import kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineFactory
import kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineImpl
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

/**
 * Ports a few jsr223-specific main-kts tests (see `MainKtsJsr223Test` in `kotlin-main-kts-test`) to
 * exercise [KotlinJsr223DaemonScriptEngineFactory]'s custom-script-definition support with a real,
 * non-trivial script definition ([MainKtsScript]), rather than the plain, definition-less default
 * every other test in this module uses.
 *
 * The engine is manually instantiated via [KotlinJsr223DaemonScriptEngineFactory] rather than
 * looked up through `javax.script.ScriptEngineManager`. See [KotlinJsr223DaemonScriptEngineTest]'s
 * KDoc for why. Only the scenario that does not depend on functionality out of this pipeline's
 * scope is actually run; the other two are ported as [Disabled] tests documenting why.
 */
class KotlinJsr223DaemonScriptEngineMainKtsTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val compilerClasspath: List<File> = classpathFromSystemProperty("kotlinJsr223DaemonCompilerClasspath")

    private val stdlib: File by lazy {
        File(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())
    }

    // With the main-kts script definition wired in, every snippet's synthetic bindings-exposing
    // snippet declares an implicit ScriptTemplateWithBindings receiver, so kotlin-script-runtime
    // (which defines that class) must also be on the daemon compile classpath.
    private val scriptRuntime: File by lazy {
        File(kotlin.script.templates.standard.ScriptTemplateWithBindings::class.java.protectionDomain.codeSource.location.toURI())
    }

    private val mainKtsScriptDefinition = createJvmScriptDefinitionFromTemplate<MainKtsScript>()

    // The daemon connection is leased once and cached for the engine's whole lifetime, so tests
    // must shut it down explicitly.
    private val enginesToShutDown = mutableListOf<KotlinJsr223DaemonScriptEngineImpl>()

    private fun newEngine(): KotlinJsr223DaemonScriptEngineImpl {
        val factory = KotlinJsr223DaemonScriptEngineFactory(
            compilerClasspath = compilerClasspath,
            additionalClasspath = listOf(stdlib.toPath(), scriptRuntime.toPath()),
            daemonOptions = DaemonOptions(
                runFilesPath = daemonRunDir.resolve("run").toString(),
                shutdownDelayMilliseconds = 0,
            ),
            daemonLogOptions = DaemonLogOptions(logsPath = daemonRunDir.resolve("logs").toString()),
            baseCompilationConfiguration = mainKtsScriptDefinition.compilationConfiguration,
            baseEvaluationConfiguration = mainKtsScriptDefinition.evaluationConfiguration,
        )
        return (factory.scriptEngine as KotlinJsr223DaemonScriptEngineImpl).also { enginesToShutDown += it }
    }

    @AfterEach
    fun tearDown() {
        for (engine in enginesToShutDown) {
            engine.forceShutdownDaemonForTests()
        }
        enginesToShutDown.clear()
    }

    // Port of MainKtsJsr223Test.testSimpleEval.
    @Test
    fun testSimpleEval() {
        val engine = newEngine()
        val res1 = engine.eval("val x = 3")
        assertEquals(null, res1)
        val res2 = engine.eval("x + 2")
        assertEquals(5, res2)
    }

    // Port of MainKtsJsr223Test.testWithDirectBindings. A value put directly into the engine's
    // default ScriptContext ENGINE_SCOPE Bindings is visible to a snippet as an ordinary property.
    @Test
    fun testWithDirectBindings() {
        val engine = newEngine()
        engine.put("z", 6)
        val res1 = engine.eval("val x = 7")
        assertEquals(null, res1)
        val res2 = engine.eval("z * x")
        assertEquals(42, res2)
    }

    // Port of MainKtsJsr223Test.testWithImport.
    @Test
    @Disabled(
        "MainKtsScriptDefinition's refineConfiguration hooks (MainKtsConfigurator's @file:Import " +
            "handling among them) never run on this module's out-of-process compile path. See " +
            "KotlinJsr223DaemonScriptEngineImpl's KDoc."
    )
    fun testWithImport() {
        val engine = newEngine()
        val res1 = engine.eval(
            """
                @file:Import("$TEST_DATA_ROOT/import-common.main.kts")
                @file:Import("$TEST_DATA_ROOT/import-middle.main.kts")
                sharedVar = sharedVar + 1
                sharedVar
            """.trimIndent()
        )
        assertEquals(5, res1)
    }
}

private const val TEST_DATA_ROOT = "libraries/tools/kotlin-main-kts-test/testData"

private fun classpathFromSystemProperty(propertyName: String): List<File> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { File(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")
