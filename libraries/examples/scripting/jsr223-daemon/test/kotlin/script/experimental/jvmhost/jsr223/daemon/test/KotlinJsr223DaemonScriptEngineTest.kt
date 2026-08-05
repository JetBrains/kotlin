/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon.test

import kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineFactory
import kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineImpl
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import javax.script.ScriptException

/**
 * Exercises [KotlinJsr223DaemonScriptEngineImpl] end-to-end against a real Kotlin compile daemon.
 *
 * The engine is manually instantiated via [KotlinJsr223DaemonScriptEngineFactory], not looked up
 * through `javax.script.ScriptEngineManager`: the factory is deliberately not registered as a
 * `javax.script.ScriptEngineFactory` service (see its KDoc).
 */
class KotlinJsr223DaemonScriptEngineTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val compilerClasspath: List<File> = classpathFromSystemProperty("kotlinJsr223DaemonCompilerClasspath")

    // The compiler needs the stdlib on the snippet compile classpath. Resolved from this test
    // JVM's own classpath (the module's compile/test classpath).
    private val stdlib: File by lazy {
        File(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())
    }

    // The daemon connection is leased once and cached for the engine's whole lifetime, so every
    // engine created by a test is tracked here and force-shut down in tearDown.
    private val enginesToShutDown = mutableListOf<KotlinJsr223DaemonScriptEngineImpl>()

    private fun newEngine(): KotlinJsr223DaemonScriptEngineImpl {
        val factory = KotlinJsr223DaemonScriptEngineFactory(
            compilerClasspath = compilerClasspath,
            additionalClasspath = listOf(stdlib.toPath()),
            daemonOptions = DaemonOptions(
                runFilesPath = daemonRunDir.resolve("run").toString(),
                shutdownDelayMilliseconds = 0,
            ),
            daemonLogOptions = DaemonLogOptions(logsPath = daemonRunDir.resolve("logs").toString()),
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

    // A snippet that throws at runtime must fail at evaluation (a ScriptException wrapping the
    // thrown exception), never as a daemon-side compile failure. Compiling
    // `throw RuntimeException(...)` always succeeds; only running it fails.
    @Test
    fun testSnippetThatThrowsAtRuntimeFailsAtEvalNotCompile() {
        val engine = newEngine()
        val exception = assertThrows(ScriptException::class.java) {
            engine.eval("throw RuntimeException(\"boom\")")
        }
        assertFalse(exception.message.orEmpty().startsWith("Error compiling"))
        var cause: Throwable? = exception.cause
        while (cause != null && cause !is RuntimeException) cause = cause.cause
        assertEquals("boom", cause?.message)
    }

    // A genuine compile error must still be reported as a compile failure.
    @Test
    fun testSnippetWithCompileErrorFailsAtCompileNotEval() {
        val engine = newEngine()
        val exception = assertThrows(ScriptException::class.java) {
            engine.eval("val x: Int = \"not an int\"")
        }
        assertTrue(exception.message.orEmpty().contains("Initializer type mismatch: expected 'Int', actual 'String'."))
    }

    // The tests below exercise snippet sources that would be awkward to smuggle through a single
    // CLI argument: embedded quotes, backslashes, `$`, newlines, tabs, and non-ASCII characters.

    @Test
    fun testMultilineSnippetWithQuotesAndEscapes() {
        val engine = newEngine()
        val snippet = """
            val greeting = "She said \"hello, world!\"\nLine2\tTabbed"
            val path = "C:\\Users\\test\\file.kt"
            greeting.length + path.length
        """.trimIndent()
        assertEquals("She said \"hello, world!\"\nLine2\tTabbed".length + "C:\\Users\\test\\file.kt".length, engine.eval(snippet))
    }

    @Test
    fun testSnippetWithDollarSignsAndBackticks() {
        val engine = newEngine()
        val snippet = """
            val price = "cost: ${'$'}5 (not a template)"
            val `backtick name` = 7
            price.length + `backtick name`
        """.trimIndent()
        assertEquals("cost: $5 (not a template)".length + 7, engine.eval(snippet))
    }

    @Test
    fun testSnippetWithTripleQuotedStringContainingNewlines() {
        val engine = newEngine()
        val snippet = "\n" +
                "val block = \"\"\"\n" +
                "line one\n" +
                "line \"two\" with quotes\n" +
                "line three\n" +
                "\"\"\".trimIndent()\n" +
                "block.lines().size\n"
        assertEquals(3, engine.eval(snippet))
    }

    @Test
    fun testSnippetWithUnicodeAndSpecialCharacters() {
        val engine = newEngine()
        val snippet = """
            val text = "unicode: \u00e9\u00e8\u00ea, emoji: \ud83d\ude00, quotes: '\u2018single\u2019' \u201cdouble\u201d"
            text.length
        """.trimIndent()
        val expected = "unicode: \u00e9\u00e8\u00ea, emoji: \ud83d\ude00, quotes: '\u2018single\u2019' \u201cdouble\u201d".length
        assertEquals(expected, engine.eval(snippet))
    }

    @Test
    fun testLongComplexMultilineSnippet() {
        val engine = newEngine()
        val snippet = """
            fun greet(name: String): String {
                val punctuation = if (name.contains("\"")) "!" else "."
                return "Hello, ${'$'}name${'$'}punctuation"
            }
            val names = listOf("Alice", "Bob \"the builder\"", "Eve\\Mallory")
            val greetings = names.map { greet(it) }
            greetings.joinToString(separator = "\n") { it }.lines().size
        """.trimIndent()
        assertEquals(3, engine.eval(snippet))
    }
}

private fun classpathFromSystemProperty(propertyName: String): List<File> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { File(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")
