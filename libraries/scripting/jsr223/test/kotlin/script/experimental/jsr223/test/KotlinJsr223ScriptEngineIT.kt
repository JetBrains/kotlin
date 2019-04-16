/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223.test

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.junit.Assert
import org.junit.Test
import javax.script.*
import kotlin.script.experimental.jsr223.KotlinJsr223ScriptEngine

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
        Assert.assertNotNull(engine as? KotlinJsr223ScriptEngine)
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
    fun testEngineRepeatWithReset() {
        val code = "open class A {}\n" +
                    "class B : A() {}"
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngine

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
        val res3 = invocator!!.invokeMethod(res1, "fn1", 3)
        Assert.assertEquals(6, res3)
    }

    @Test
    fun testSimpleCompilable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngine
        val comp1 = engine.compile("val x = 3")
        val comp2 = engine.compile("x + 2")
        val res1 = comp1.eval()
        Assert.assertNull(res1)
        val res2 = comp2.eval()
        Assert.assertEquals(5, res2)
    }

    @Test
    fun testMultipleCompilable() {
        val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223ScriptEngine
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
