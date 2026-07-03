/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon.test

import kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineFactory
import kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Exercises [KotlinJsr223DaemonScriptEngineImpl] end-to-end, against a **real Kotlin compile
 * daemon** (not an in-process/faked transport) -- mirroring how `KotlinJsr223BtaScriptEngineTest`
 * (`:kotlin-scripting-jsr223-bta`) exercises its BTA-backed counterpart.
 *
 * The engine is **manually instantiated** via [KotlinJsr223DaemonScriptEngineFactory], not looked
 * up through `javax.script.ScriptEngineManager`: the factory is deliberately not registered as a
 * `javax.script.ScriptEngineFactory` service (see its KDoc).
 */
class KotlinJsr223DaemonScriptEngineTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val compilerClasspath: List<File> = classpathFromSystemProperty("kotlinJsr223DaemonCompilerClasspath")

    // The compiler needs the stdlib on the snippet's compile classpath. Resolved from this test
    // JVM's own classpath (rather than a dedicated implementation classloader, as the BTA test
    // needs) since this module never loads a separate compiler implementation in-process -- the
    // stdlib version used here is simply the one on this module's own compile/test classpath.
    private val stdlib: File by lazy {
        File(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())
    }

    private fun newEngine(): KotlinJsr223DaemonScriptEngineImpl {
        val factory = KotlinJsr223DaemonScriptEngineFactory(
            compilerClasspath = compilerClasspath,
            additionalClasspath = listOf(stdlib.toPath()),
            daemonRunFilesPath = daemonRunDir.resolve("run").toFile(),
            daemonLogsPath = daemonRunDir.resolve("logs").toFile(),
            shutdownDelayMilliseconds = 0,
        )
        return factory.scriptEngine as KotlinJsr223DaemonScriptEngineImpl
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

private fun classpathFromSystemProperty(propertyName: String): List<File> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { File(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")
