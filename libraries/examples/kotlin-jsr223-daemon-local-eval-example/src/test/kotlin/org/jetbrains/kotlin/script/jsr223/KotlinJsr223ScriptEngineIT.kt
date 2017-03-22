/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script.jsr223

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.daemon.common.threadCpuTime
import org.junit.Assert
import org.junit.Test
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

class KotlinJsr223DaemonCompileScriptEngineIT {

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
        Assert.assertNotNull(engine as? KotlinJsr223JvmDaemonCompileScriptEngine)
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


    // Note: the test is flaky, because it is statistical and the thresholds are not big enough.
    // Therefore it was decided to disable it, but leave in the code in order to be able to quickly check overheads when needed.
//    @Test
    fun testEvalInEvalBench() {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        val script = "val x = 3\nx + 10"

        val mxBeans = ManagementFactory.getThreadMXBean()

        engine.eval("val bnd = createBindings()")

        val times = generateSequence {
            val t0 = mxBeans.threadCpuTime()

            val res1 = engine.eval(script)
            val t1 = mxBeans.threadCpuTime()

            val res2 = engine.eval("eval(\"\"\"$script\"\"\", bnd)")
            val t2 = mxBeans.threadCpuTime()

            Triple(t1 - t0, t2 - t1, t2 - t1)
        }.take(10).toList()

        val adjustedMaxDiff = times.sortedByDescending { (_, _, diff) -> diff }.drop(2).first()

        fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
        Assert.assertTrue("eval in eval is too long: ${times.joinToString { "(${it.first.ms()}, ${it.second.ms()})" }} (expecting no more than 5x difference)",
                adjustedMaxDiff.third < 10 || adjustedMaxDiff.first * 5 > adjustedMaxDiff.second )
    }
}

fun assertThrows(exceptionClass: Class<*>, body: () -> Unit) {
    try {
        body()
        Assert.fail("Expecting an exception of type ${exceptionClass.name}")
    }
    catch (e: Throwable) {
        if (!exceptionClass.isAssignableFrom(e.javaClass)) {
            Assert.fail("Expecting an exception of type ${exceptionClass.name} but got ${e.javaClass.name}")
        }
    }
}
