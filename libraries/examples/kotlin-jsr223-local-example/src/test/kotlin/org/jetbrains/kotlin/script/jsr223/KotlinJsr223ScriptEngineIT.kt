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

import org.jetbrains.kotlin.cli.common.KotlinVersion
import org.junit.Assert
import org.junit.Test
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

class KotlinJsr223ScriptEngineIT {

    @Test
    fun testEngineFactory() {
        val factory = ScriptEngineManager().getEngineByExtension("kts").factory
        Assert.assertNotNull(factory)
        factory!!.apply {
            Assert.assertEquals("kotlin", languageName)
            Assert.assertEquals(KotlinVersion.VERSION, languageVersion)
            Assert.assertEquals("kotlin", engineName)
            Assert.assertEquals(KotlinVersion.VERSION, engineVersion)
            Assert.assertEquals(listOf("kts"), extensions)
            Assert.assertEquals(listOf("text/x-kotlin"), mimeTypes)
            Assert.assertEquals(listOf("kotlin"), names)
            Assert.assertEquals("obj.method(arg1, arg2, arg3)", getMethodCallSyntax("obj", "method", "arg1", "arg2", "arg3"))
            Assert.assertEquals("print(\"Hello, world!\")", getOutputStatement("Hello, world!"))
            Assert.assertEquals(KotlinVersion.VERSION, getParameter(ScriptEngine.LANGUAGE_VERSION))
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
        Assert.assertNotNull(engine as? KotlinJsr223JvmLocalScriptEngine)
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
}