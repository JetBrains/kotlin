/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223.test

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.scripting.compiler.plugin.runAndCheckResults
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.createTempFile
import javax.script.*
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl
import kotlin.script.templates.standard.ScriptTemplateWithBindings
import kotlin.test.*

// duplicating it here to avoid dependency on the implementation - it may interfere with tests
private const val KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY = "kotlin.jsr223.experimental.resolve.dependencies.from.context.classloader"

@Suppress("unused") // accessed from the tests below
val shouldBeVisibleFromRepl = 7

@Suppress("unused") // accessed from the tests below
fun callLambda(x: Int, aFunction: (Int) -> Int): Int = aFunction.invoke(x)

@Suppress("unused") // accessed from the tests below
inline fun inlineCallLambda(x: Int, aFunction: (Int) -> Int): Int = aFunction.invoke(x)

@Suppress("unused", "UNCHECKED_CAST") // accessed from the tests below
fun ScriptTemplateWithBindings.myFunFromBindings(n: Int): Int =
    (bindings["myFunFromBindings"] as (Int) -> Int).invoke(n)


class KotlinJsr223ScriptEngineIT {

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun testEngineFactory() {
        val factory = ScriptEngineManager().getEngineByExtension("kts").factory
        assertNotNull(factory)
        factory.apply {
            assertEquals("kotlin", languageName)
            assertEquals(KotlinCompilerVersion.VERSION, languageVersion)
            assertEquals("kotlin", engineName)
            assertEquals(KotlinCompilerVersion.VERSION, engineVersion)
            assertEquals(listOf("kts"), extensions)
            assertEquals(listOf("text/x-kotlin"), mimeTypes)
            assertEquals(listOf("kotlin"), names)
            assertEquals("obj.method(arg1, arg2, arg3)", getMethodCallSyntax("obj", "method", "arg1", "arg2", "arg3"))
            assertEquals("print(\"Hello, world!\")", getOutputStatement("Hello, world!"))
            assertEquals(KotlinCompilerVersion.VERSION, getParameter(ScriptEngine.LANGUAGE_VERSION))
            val sep = System.getProperty("line.separator")
            val prog = arrayOf("val x: Int = 3", "var y = x + 2")
            assertEquals(prog.joinToString(sep) + sep, getProgram(*prog))
        }
    }

    @Test
    fun testEngine() {
        val factory = ScriptEngineManager().getEngineByExtension("kts").factory
        assertNotNull(factory)
        val engine = factory.scriptEngine
        assertNotNull(engine as? KotlinJsr223ScriptEngineImpl)
        assertSame(factory, engine.factory)
        val bindings = engine.createBindings()
        assertTrue(bindings is SimpleBindings)
    }

    @Test
    fun testSimpleEval() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res1 = engine.eval("val x = 3")
        assertNull(res1)
        val res2 = engine.eval("x + 2")
        assertEquals(5, res2)
    }

    @Test
    @Ignore // Probably not possible to make it sensible on CI and with parallel run, so leaving it here for manual testing only
    fun testMemory() {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()!!
        var prevMem = memoryMXBean.getHeapMemoryUsage().getUsed()
        for (i in 1..10) {
            with(ScriptEngineManager().getEngineByExtension("kts")) {
                val res1 = eval("val x = 3")
                assertNull(res1)
                val res2 = eval("x + 2")
                assertEquals(5, res2)
            }
            System.gc()
            val curMem = memoryMXBean.getHeapMemoryUsage().getUsed()
            if (i > 3 && curMem > prevMem) {
                assertTrue((curMem - prevMem) < 1024*1024, "Memory leak: iter: $i prev: $prevMem, cur: $curMem")
            }
            println("${curMem/1024/1024}Mb")
            prevMem = curMem
        }
    }


    @Test
    fun testIncomplete() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res0 = try {
            engine.eval("val x =")
        } catch (e: ScriptException) {
            e
        }
        assertTrue((res0 as? ScriptException)?.message?.contains("Expecting an expression") ?: false, "Unexpected check results: $res0")
    }

    @Test
    fun testEvalWithError() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        try {
            engine.eval("java.lang.fish")
            fail("Script error expected")
        } catch (_: ScriptException) {}

        val res1 = engine.eval("val x = 3")
        assertNull(res1)

        try {
            engine.eval("y")
            fail("Script error expected")
        } catch (e: ScriptException) {
            assertTrue(
                e.message?.contains("Unresolved reference: y") ?: false,
                "Expected message to contain \"Unresolved reference: y\", actual: \"${e.message}\""
            )
        }

        val res3 = engine.eval("x + 2")
        assertEquals(5, res3)
    }

    @Test
    fun testEvalWithException() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        try {
            engine.eval("throw Exception(\"!!\")")
            fail("Expecting exception to propagate")
        } catch (e: ScriptException) {
            assertEquals("!!", e.cause?.message)
        }
        // engine should remain operational
        val res1 = engine.eval("val x = 3")
        assertNull(res1)
        val res2 = engine.eval("x + 4")
        assertEquals(7, res2)
    }


    @Test
    fun testEngineRepeatWithReset() {
        val code = "open class A {}\n" +
                    "class B : A() {}"
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngineImpl

        val res1 = engine.eval(code)
        assertNull(res1)

        engine.state.history.reset()

        engine.eval(code)
    }

    @Test
    fun testInvocable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res0 = engine.eval("""
fun fn(x: Int) = x + 2
val obj = object {
    fun fn1(x: Int) = x + 3
}
obj
""")
        assertNotNull(res0)
        val invocator = engine as? Invocable
        assertNotNull(invocator)
        val res1 = invocator.invokeFunction("fn", 6)
        assertEquals(8, res1)
        assertThrows(NoSuchMethodException::class.java) {
            invocator.invokeFunction("fn1", 3)
        }
        val res2 = invocator.invokeFunction("fn", 3)
        assertEquals(5, res2)
        assertThrows(NoSuchMethodException::class.java) {
            invocator.invokeMethod(res0, "fn", 3)
        }
        val res3 = invocator.invokeMethod(res0, "fn1", 3)
        assertEquals(6, res3)
    }

    @Test
    fun testSimpleCompilable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngineImpl
        val comp1 = engine.compile("val x = 3")
        val comp2 = engine.compile("x + 2")
        val res1 = comp1.eval()
        assertNull(res1)
        val res2 = comp2.eval()
        assertEquals(5, res2)
    }

    @Test
    fun testSimpleCompilableWithBindings() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        engine.put("z", 33)
        val comp1 = (engine as Compilable).compile("val x = 10 + bindings[\"z\"] as Int\nx + 20")
        val comp2 = (engine as Compilable).compile("val x = 10 + z\nx + 20")
        val res1 = comp1.eval()
        assertEquals(63, res1)
        val res12 = comp2.eval()
        assertEquals(63, res12)
        engine.put("z", 44)
        val res2 = comp1.eval()
        assertEquals(74, res2)
        val res22 = comp2.eval()
        assertEquals(74, res22)
    }

    @Test
    fun testMultipleCompilable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngineImpl
        val compiled1 = engine.compile("""listOf(1,2,3).joinToString(",")""")
        val compiled2 = engine.compile("""val x = bindings["boundValue"] as Int + bindings["z"] as Int""")
        val compiled3 = engine.compile("""x""")

        assertEquals("1,2,3", compiled1.eval())
        assertEquals("1,2,3", compiled1.eval())
        assertEquals("1,2,3", compiled1.eval())
        assertEquals("1,2,3", compiled1.eval())

        engine.getBindings(ScriptContext.ENGINE_SCOPE).apply {
            put("boundValue", 100)
            put("z", 33)
        }

        compiled2.eval()

        assertEquals(133, compiled3.eval())
        assertEquals(133, compiled3.eval())
        assertEquals(133, compiled3.eval())
    }

    @Test
    fun testEvalWithContext() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        engine.put("z", 33)

        engine.eval("""val x = 10 + bindings["z"] as Int""")

        val result = engine.eval("""x + 20""")
        assertEquals(63, result)

        // in the current implementation the history is shared between contexts, so "x" could also be used in this line,
        // but this behaviour probably will not be preserved in the future, since contexts may become completely isolated
        val result2 = engine.eval("""11 + bindings["boundValue"] as Int""", engine.createBindings().apply {
            put("boundValue", 100)
        })
        assertEquals(111, result2)

        engine.put("nullable", null)
        val result3 = engine.eval("bindings[\"nullable\"]?.let { it as Int } ?: -1")
        assertEquals(-1, result3)
    }

    @Test
    fun testEvalWithContextDirect() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        engine.put("z", 33)

        engine.eval("val x = 10 + z")

        val result = engine.eval("x + 20")
        assertEquals(63, result)

        // in the current implementation the history is shared between contexts, so "x" could also be used in this line,
        // but this behaviour probably will not be preserved in the future, since contexts may become completely isolated
        val result2 = engine.eval("11 + boundValue", engine.createBindings().apply {
            put("boundValue", 100)
        })
        assertEquals(111, result2)

        engine.put("nullable", null)
        val result3 = engine.eval("nullable?.let { it as Int } ?: -1")
        assertEquals(-1, result3)
    }

    @Test
    fun testEvalWithContextNamesWithSymbols() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        engine.put("\u263a", 2)
        engine.put("a.b", 3)
        engine.put("c:d", 5)
        engine.put("e;f", 7)
        engine.put("g\$h", 11)
        engine.put("i<j", 13)
        engine.put("k>l", 17)
        engine.put("m[n", 19)
        engine.put("o]p", 23)
        engine.put("q/r", 29)
        engine.put("s\\t", 31)
        engine.put("u v", 37)
        engine.put(" ", 41)
        engine.put("    ", 43)

        assertEquals(4, engine.eval("`\u263a` * 2"))
        assertEquals(5, engine.eval("2 + `a\\,b`"))
        assertEquals(2, engine.eval("`a\\,b` - 1"))
        assertEquals(6, engine.eval("1 + `c\\!d`"))
        assertEquals(7, engine.eval("`e\\?f`"))
        assertEquals(11, engine.eval("`g\\%h`"))
        assertEquals(13, engine.eval("`i\\^j`"))
        assertEquals(17, engine.eval("`k\\_l`"))
        assertEquals(19, engine.eval("`m\\{n`"))
        assertEquals(23, engine.eval("`o\\}p`"))
        assertEquals(29, engine.eval("`q\\|r`"))
        assertEquals(31, engine.eval("`s\\-t`"))
        assertEquals(37, engine.eval("`u v`"))
        assertEquals(41, engine.eval("`_`"))
        assertEquals(43, engine.eval("`____`"))
    }

    @Test
    fun testSimpleEvalInEval() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res1 = engine.eval("val x = 3")
        assertNull(res1)
        val res2 = engine.eval("val y = eval(\"\$x + 2\") as Int\ny")
        assertEquals(5, res2)
        val res3 = engine.eval("y + 2")
        assertEquals(7, res3)
    }

    @Test
    fun testEvalInEvalWithBindingsWithLambda() {
        // the problem (KT-67747) is only reproducible with INDY lambdas
        withProperty(
            "kotlin.script.base.compiler.arguments",
            getPropertyValue = { listOfNotNull(it?.takeIf(String::isNotBlank), "-Xlambdas=indy").joinToString(" ") }
        ) {
            val engine = ScriptEngineManager().getEngineByExtension("kts")!!
            // code here is somewhat similar to one used in Spring's KotlinScriptTemplateTests
            val res1 = engine.eval(
                """
            fun f(script: String): Int {
                val bindings = javax.script.SimpleBindings()
                bindings.put("myFunFromBindings", { i: Int -> i * 7 })
                return eval(script, bindings) as Int
            }
            """.trimIndent()
            )
            assertNull(res1)
            // Note that direct call to the lambda stored in bindings is not possible, so the additional helper
            // [kotlin.script.experimental.jsr223.test.myFunFromBindings] is used (as in Spring)
            val script = """
            import kotlin.script.experimental.jsr223.test.*
            myFunFromBindings(6)
            """.trimIndent()
            val res2 = (engine as Invocable).invokeFunction("f", script)
            assertEquals(42, res2)
        }
    }

    @Test
    fun `kotlin script evaluation should support functional return types`() {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!

        val script = "{1 + 2}"
        val result = scriptEngine.eval(script)

        assertTrue(result is Function0<*>)
        assertEquals(3, (result as Function0<*>).invoke())
    }

    @Test
    fun testResolveFromContextStandard() {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!
        val result = scriptEngine.eval("kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl * 6")
        assertEquals(42, result)
    }

    @Test
    fun testResolveFromContextLambda() {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!

        val script1 = """
            kotlin.script.experimental.jsr223.test.callLambda(4) { x -> 
                x % aValue
            }
        """

        val script2 = """
            kotlin.script.experimental.jsr223.test.inlineCallLambda(5) { x ->
                x % aValue
            }
        """

        scriptEngine.put("aValue", 3)

        val res1 = scriptEngine.eval(script1)
        assertEquals(1, res1)
        val res2 = scriptEngine.eval(script2)
        assertEquals(2, res2)
    }

    @Test
    fun testResolveFromContextDirectExperimental() {
        val prevProp = System.setProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY, "true")
        try {
            val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!
            val result = scriptEngine.eval("kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl * 6")
            assertEquals(42, result)
        } finally {
            if (prevProp == null) System.clearProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY)
            else System.setProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY, prevProp)
        }
    }

    @Test
    fun testInliningInJdk171() {
        val jdk17 = try {
            KtTestUtil.getJdk17Home()
        } catch (_: NoClassDefFoundError) {
            println("IGNORED: Test infrastructure doesn't work yet with embeddable compiler")
            return
        }
        val javaExe = if (System.getProperty("os.name").contains("windows", ignoreCase = true)) "java.exe" else "java"
        val runtime = File(jdk17, "bin" + File.separator + javaExe)

        val tempDir = createTempDirectory(KotlinJsr223ScriptEngineIT::class.simpleName!!)
        try {
            val outJar = createTempFile(tempDir, "inlining17", ".jar").toFile()
            val compileCp = System.getProperty("testCompilationClasspath")!!.split(File.pathSeparator).map(::File)
            assertTrue(
                compileCp.any { it.name.startsWith("kotlin-stdlib") },
                "Expecting \"testCompilationClasspath\" property to contain stdlib jar:\n$compileCp"
            )
            val paths = PathUtil.kotlinPathsForDistDirectory
            runAndCheckResults(
                listOf(
                    runtime.absolutePath,
                    "-cp", paths.compilerClasspath.joinToString(File.pathSeparator),
                    K2JVMCompiler::class.java.name,
                    K2JVMCompilerArguments::noStdlib.cliArgument,
                    K2JVMCompilerArguments::classpath.cliArgument, compileCp.joinToString(File.pathSeparator) { it.path },
                    K2JVMCompilerArguments::destination.cliArgument, outJar.absolutePath,
                    K2JVMCompilerArguments::jvmTarget.cliArgument, "17",
                    "libraries/scripting/jsr223-test/testData/testJsr223Inlining.kt"
                ),
                additionalEnvVars = listOf("JAVA_HOME" to jdk17.absolutePath)
            )

            val runtimeCp = System.getProperty("testJsr223RuntimeClasspath")!!.split(File.pathSeparator).map(::File) + outJar
            assertTrue(
                runtimeCp.any { it.name.startsWith("kotlin-scripting-jsr223") },
                "Expecting \"testJsr223RuntimeClasspath\" property to contain JSR223 jar:\n$runtimeCp"
            )

            runAndCheckResults(
                listOf(runtime.absolutePath, "-cp", runtimeCp.joinToString(File.pathSeparator) { it.path }, "TestJsr223InliningKt"),
                listOf("OK")
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testEvalWithCompilationError() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        val compilable: Compilable = engine as Compilable
        assertThrows(ScriptException::class.java) {
            compilable.compile("foo")
        }
        compilable.compile("true")
        engine.eval("val x = 3")
        compilable.compile("x")
    }
}

fun assertThrows(exceptionClass: Class<*>, body: () -> Unit) {
    try {
        body()
        fail("Expecting an exception of type ${exceptionClass.name}")
    } catch (e: Throwable) {
        if (!exceptionClass.isAssignableFrom(e.javaClass)) {
            fail("Expecting an exception of type ${exceptionClass.name} but got ${e.javaClass.name}")
        }
    }
}

internal fun <T> withProperty(name: String, getPropertyValue: (String?) -> String?, body: () -> T): T {
    val prevPropertyVal = System.getProperty(name)
    val value = getPropertyValue(prevPropertyVal)
    when (value) {
        null -> System.clearProperty(name)
        else -> System.setProperty(name, value)
    }
    try {
        return body()
    } finally {
        when (prevPropertyVal) {
            null -> System.clearProperty(name)
            else -> System.setProperty(name, prevPropertyVal)
        }
    }
}
