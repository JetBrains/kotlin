/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223.test

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.lang.management.ManagementFactory
import javax.script.*
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

// duplicating it here to avoid dependency on the implementation - it may interfere with tests
private const val KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY = "kotlin.jsr223.experimental.resolve.dependencies.from.context.classloader"

@Suppress("unused") // accessed from the tests below
val shouldBeVisibleFromRepl = 7

@Suppress("unused") // accessed from the tests below
fun callLambda(x: Int, aFunction: (Int) -> Int): Int = aFunction.invoke(x)

@Suppress("unused") // accessed from the tests below
inline fun inlineCallLambda(x: Int, aFunction: (Int) -> Int): Int = aFunction.invoke(x)

class KotlinJsr223ScriptEngineIT {

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun testEngineFactory() {
        val factory = ScriptEngineManager().getEngineByExtension("kts").factory
        Assert.assertNotNull(factory)
        factory!!.apply {
            Assert.assertEquals("kotlin", languageName)
            Assert.assertEquals(KotlinCompilerVersion.VERSION, languageVersion)
            Assert.assertEquals("kotlin", engineName)
            Assert.assertEquals(KotlinCompilerVersion.VERSION, engineVersion)
            Assert.assertEquals(listOf("kts"), extensions)
            Assert.assertEquals(listOf("text/x-kotlin"), mimeTypes)
            Assert.assertEquals(listOf("kotlin"), names)
            Assert.assertEquals("obj.method(arg1, arg2, arg3)", getMethodCallSyntax("obj", "method", "arg1", "arg2", "arg3"))
            Assert.assertEquals("print(\"Hello, world!\")", getOutputStatement("Hello, world!"))
            Assert.assertEquals(KotlinCompilerVersion.VERSION, getParameter(ScriptEngine.LANGUAGE_VERSION))
            val sep = System.getProperty("line.separator")
            val prog = arrayOf("val x: Int = 3", "var y = x + 2")
            Assert.assertEquals(prog.joinToString(sep) + sep, getProgram(*prog))
        }
    }

    @Test
    fun testEngine() {
        val factory = ScriptEngineManager().getEngineByExtension("kts").factory
        Assert.assertNotNull(factory)
        val engine = factory!!.scriptEngine
        Assert.assertNotNull(engine as? KotlinJsr223ScriptEngineImpl)
        Assert.assertSame(factory, engine!!.factory)
        val bindings = engine.createBindings()
        Assert.assertTrue(bindings is SimpleBindings)
    }

    @Test
    fun testSimpleEval() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res1 = engine.eval("val x = 3")
        Assert.assertNull(res1)
        val res2 = engine.eval("x + 2")
        Assert.assertEquals(5, res2)
    }

    @Test
    @Ignore // Probably not possible to make it sensible on CI and with parallel run, so leaving it here for manual testing only
    fun testMemory() {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()!!
        var prevMem = memoryMXBean.getHeapMemoryUsage().getUsed()
        for (i in 1..10) {
            with(ScriptEngineManager().getEngineByExtension("kts")) {
                val res1 = eval("val x = 3")
                Assert.assertNull(res1)
                val res2 = eval("x + 2")
                Assert.assertEquals(5, res2)
            }
            System.gc()
            val curMem = memoryMXBean.getHeapMemoryUsage().getUsed()
            if (i > 3 && curMem > prevMem) {
                Assert.assertTrue("Memory leak: iter: $i prev: $prevMem, cur: $curMem", (curMem - prevMem) < 1024*1024 )
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
        Assert.assertTrue("Unexpected check results: $res0", (res0 as? ScriptException)?.message?.contains("Expecting an expression") ?: false)
    }

    @Test
    fun testEvalWithError() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        try {
            engine.eval("java.lang.fish")
            Assert.fail("Script error expected")
        } catch (e: ScriptException) {}

        val res1 = engine.eval("val x = 3")
        Assert.assertNull(res1)

        try {
            engine.eval("y")
            Assert.fail("Script error expected")
        } catch (e: ScriptException) {
            Assert.assertTrue(
                "Expected message to contain \"Unresolved reference: y\", actual: \"${e.message}\"",
                e.message?.contains("Unresolved reference: y") ?: false
            )
        }

        val res3 = engine.eval("x + 2")
        Assert.assertEquals(5, res3)
    }

    @Test
    fun testEvalWithException() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        try {
            engine.eval("throw Exception(\"!!\")")
            Assert.fail("Expecting exception to propagate")
        } catch (e: ScriptException) {
            Assert.assertEquals("!!", e.cause?.message)
        }
        // engine should remain operational
        val res1 = engine.eval("val x = 3")
        Assert.assertNull(res1)
        val res2 = engine.eval("x + 4")
        Assert.assertEquals(7, res2)
    }


    @Test
    fun testEngineRepeatWithReset() {
        val code = "open class A {}\n" +
                    "class B : A() {}"
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngineImpl

        val res1 = engine.eval(code)
        Assert.assertNull(res1)

        engine.state.history.reset()

        engine.eval(code)
    }

    @Test
    fun testInvocable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res1 = engine.eval("""
fun fn(x: Int) = x + 2
val obj = object {
    fun fn1(x: Int) = x + 3
}
obj
""")
        Assert.assertNotNull(res1)
        val invocator = engine as? Invocable
        Assert.assertNotNull(invocator)
        assertThrows(NoSuchMethodException::class.java) {
            invocator!!.invokeFunction("fn1", 3)
        }
        val res2 = invocator!!.invokeFunction("fn", 3)
        Assert.assertEquals(5, res2)
        // TODO: fix and restore
//        assertThrows(NoSuchMethodException::class.java) {
//            invocator!!.invokeMethod(res1, "fn", 3)
//        }
        val res3 = invocator.invokeMethod(res1, "fn1", 3)
        Assert.assertEquals(6, res3)
    }

    @Test
    fun testSimpleCompilable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngineImpl
        val comp1 = engine.compile("val x = 3")
        val comp2 = engine.compile("x + 2")
        val res1 = comp1.eval()
        Assert.assertNull(res1)
        val res2 = comp2.eval()
        Assert.assertEquals(5, res2)
    }

    @Test
    fun testSimpleCompilableWithBindings() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")
        engine.put("z", 33)
        val comp = (engine as Compilable).compile("val x = 10 + bindings[\"z\"] as Int\nx + 20")
        val res1 = comp.eval()
        Assert.assertEquals(63, res1)
        engine.put("z", 44)
        val res2 = comp.eval()
        Assert.assertEquals(74, res2)
    }

    @Test
    fun testMultipleCompilable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngineImpl
        val compiled1 = engine.compile("""listOf(1,2,3).joinToString(",")""")
        val compiled2 = engine.compile("""val x = bindings["boundValue"] as Int + bindings["z"] as Int""")
        val compiled3 = engine.compile("""x""")

        Assert.assertEquals("1,2,3", compiled1.eval())
        Assert.assertEquals("1,2,3", compiled1.eval())
        Assert.assertEquals("1,2,3", compiled1.eval())
        Assert.assertEquals("1,2,3", compiled1.eval())

        engine.getBindings(ScriptContext.ENGINE_SCOPE).apply {
            put("boundValue", 100)
            put("z", 33)
        }

        compiled2.eval()

        Assert.assertEquals(133, compiled3.eval())
        Assert.assertEquals(133, compiled3.eval())
        Assert.assertEquals(133, compiled3.eval())
    }

    @Test
    fun testEvalWithContext() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        engine.put("z", 33)

        engine.eval("""val x = 10 + bindings["z"] as Int""")

        val result = engine.eval("""x + 20""")
        Assert.assertEquals(63, result)

        // in the current implementation the history is shared between contexts, so "x" could also be used in this line,
        // but this behaviour probably will not be preserved in the future, since contexts may become completely isolated
        val result2 = engine.eval("""11 + bindings["boundValue"] as Int""", engine.createBindings().apply {
            put("boundValue", 100)
        })
        Assert.assertEquals(111, result2)
    }

    @Test
    fun testEvalWithContextDirect() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        engine.put("z", 33)

        engine.eval("val x = 10 + z")

        val result = engine.eval("x + 20")
        Assert.assertEquals(63, result)

        // in the current implementation the history is shared between contexts, so "x" could also be used in this line,
        // but this behaviour probably will not be preserved in the future, since contexts may become completely isolated
        val result2 = engine.eval("11 + boundValue", engine.createBindings().apply {
            put("boundValue", 100)
        })
        Assert.assertEquals(111, result2)
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

        Assert.assertEquals(4, engine.eval("`\u263a` * 2"))
        Assert.assertEquals(5, engine.eval("2 + `a\\,b`"))
        Assert.assertEquals(2, engine.eval("`a\\,b` - 1"))
        Assert.assertEquals(6, engine.eval("1 + `c\\!d`"))
        Assert.assertEquals(7, engine.eval("`e\\?f`"))
        Assert.assertEquals(11, engine.eval("`g\\%h`"))
        Assert.assertEquals(13, engine.eval("`i\\^j`"))
        Assert.assertEquals(17, engine.eval("`k\\_l`"))
        Assert.assertEquals(19, engine.eval("`m\\{n`"))
        Assert.assertEquals(23, engine.eval("`o\\}p`"))
        Assert.assertEquals(29, engine.eval("`q\\|r`"))
        Assert.assertEquals(31, engine.eval("`s\\-t`"))
        Assert.assertEquals(37, engine.eval("`u v`"))
        Assert.assertEquals(41, engine.eval("`_`"))
        Assert.assertEquals(43, engine.eval("`____`"))
    }

    @Test
    fun testSimpleEvalInEval() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        val res1 = engine.eval("val x = 3")
        Assert.assertNull(res1)
        val res2 = engine.eval("val y = eval(\"\$x + 2\") as Int\ny")
        Assert.assertEquals(5, res2)
        val res3 = engine.eval("y + 2")
        Assert.assertEquals(7, res3)
    }

    @Test
    fun `kotlin script evaluation should support functional return types`() {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!

        val script = "{1 + 2}"
        val result = scriptEngine.eval(script)

        Assert.assertTrue(result is Function0<*>)
        Assert.assertEquals(3, (result as Function0<*>).invoke())
    }

    @Test
    fun testResolveFromContextStandard() {
        val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!
        val result = scriptEngine.eval("kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl * 6")
        Assert.assertEquals(42, result)
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
        Assert.assertEquals(1, res1)
        val res2 = scriptEngine.eval(script2)
        Assert.assertEquals(2, res2)
    }

    @Test
    fun testResolveFromContextDirectExperimental() {
        val prevProp = System.setProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY, "true")
        try {
            val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")!!
            val result = scriptEngine.eval("kotlin.script.experimental.jsr223.test.shouldBeVisibleFromRepl * 6")
            Assert.assertEquals(42, result)
        } finally {
            if (prevProp == null) System.clearProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY)
            else System.setProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY, prevProp)
        }
    }
}

fun assertThrows(exceptionClass: Class<*>, body: () -> Unit) {
    try {
        body()
        Assert.fail("Expecting an exception of type ${exceptionClass.name}")
    } catch (e: Throwable) {
        if (!exceptionClass.isAssignableFrom(e.javaClass)) {
            Assert.fail("Expecting an exception of type ${exceptionClass.name} but got ${e.javaClass.name}")
        }
    }
}
