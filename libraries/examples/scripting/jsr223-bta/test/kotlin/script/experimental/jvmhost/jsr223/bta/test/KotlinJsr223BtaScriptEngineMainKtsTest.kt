/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta.test

import kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineFactory
import kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineImpl
import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate

/**
 * Ports a few of `MainKtsJsr223Test`'s cases to exercise custom-script-definition support with a
 * real, non-trivial definition ([MainKtsScript]), rather than the definition-less default the other
 * tests in this module use.
 */
class KotlinJsr223BtaScriptEngineMainKtsTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val mainKtsScriptDefinition = createJvmScriptDefinitionFromTemplate<MainKtsScript>()

    private val enginesToClose = mutableListOf<KotlinJsr223BtaScriptEngineImpl>()

    private fun newEngine(): KotlinJsr223BtaScriptEngineImpl {
        val factory = KotlinJsr223BtaScriptEngineFactory(
            compilerClasspath = classpathFromSystemProperty("kotlinJsr223BtaImplClasspath"),
            scriptingPluginClasspath = classpathFromSystemProperty("kotlinJsr223BtaScriptingPluginClasspath"),
            additionalClasspath = listOf(stdlibPath, scriptRuntimePath),
            daemonRunFilesPath = daemonRunDir.resolve("run"),
            daemonLogsPath = daemonRunDir.resolve("logs"),
            daemonShutdownDelayMillis = 0,
            baseCompilationConfiguration = mainKtsScriptDefinition.compilationConfiguration,
            baseEvaluationConfiguration = mainKtsScriptDefinition.evaluationConfiguration,
        )
        return (factory.scriptEngine as KotlinJsr223BtaScriptEngineImpl).also { enginesToClose += it }
    }

    @AfterEach
    fun tearDown() {
        for (engine in enginesToClose) {
            engine.close()
        }
        enginesToClose.clear()
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

    // Port of MainKtsJsr223Test.testWithDirectBindings.
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
            "KotlinJsr223BtaScriptEngineImpl's KDoc."
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
