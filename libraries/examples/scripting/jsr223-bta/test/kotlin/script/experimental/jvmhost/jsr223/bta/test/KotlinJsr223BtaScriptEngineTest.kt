/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta.test

import kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineFactory
import kotlin.script.experimental.jvmhost.jsr223.bta.KotlinJsr223BtaScriptEngineImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import javax.script.ScriptException

/**
 * Exercises [KotlinJsr223BtaScriptEngineImpl] end-to-end against a real Build Tools API compilation.
 * The engine is instantiated manually, the factory not being registered as a
 * `javax.script.ScriptEngineFactory` service.
 */
class KotlinJsr223BtaScriptEngineTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val enginesToClose = mutableListOf<KotlinJsr223BtaScriptEngineImpl>()

    private fun newEngine(): KotlinJsr223BtaScriptEngineImpl {
        val factory = KotlinJsr223BtaScriptEngineFactory(
            compilerClasspath = classpathFromSystemProperty("kotlinJsr223BtaImplClasspath"),
            scriptingPluginClasspath = classpathFromSystemProperty("kotlinJsr223BtaScriptingPluginClasspath"),
            additionalClasspath = listOf(stdlibPath),
            daemonRunFilesPath = daemonRunDir.resolve("run"),
            daemonLogsPath = daemonRunDir.resolve("logs"),
            daemonShutdownDelayMillis = 0,
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

    @Test
    fun testSnippetWithCompileErrorFailsAtCompileNotEval() {
        val engine = newEngine()
        val exception = assertThrows(ScriptException::class.java) {
            engine.eval("val x: Int = \"not an int\"")
        }
        assertTrue(exception.message.orEmpty().contains("Initializer type mismatch: expected 'Int', actual 'String'."))
    }

    // The tests below exercise snippet sources with characters that have to survive the trip to the
    // compiler process.

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
