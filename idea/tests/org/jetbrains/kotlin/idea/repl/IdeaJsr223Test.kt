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

package org.jetbrains.kotlin.idea.repl

import com.intellij.testFramework.PlatformTestCase
import org.junit.Test
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.test.assertFails

class IdeaJsr223Test : PlatformTestCase() {

    @Test
    fun testJsr223Engine() {
        val semgr = ScriptEngineManager()

        val engine = semgr.getEngineByName("kotlin")

        assertNotNull(engine)

        val res0 = assertFails { engine.eval("val x =") }
        assertTrue("Unexpected check results: $res0", (res0 as? ScriptException)?.message?.contains("incomplete code") ?: false)

        val res1 = engine.eval("val x = 5\nval y = listOf(x)")
        assertNull("Unexpected eval result: $res1", res1)

        val res2 = engine.eval("y.first() + 2")
        assertEquals(7, res2)
    }

    @Test
    fun testJsr223ScriptWithBindings() {
        val semgr = ScriptEngineManager()

        val engine = semgr.getEngineByName("kotlin")

        val bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)

        bindings.put(ScriptEngine.ARGV, arrayOf("42"))
        bindings.put("abc", 13)

        val res1 = engine.eval("2 + args[0].toInt()")
        assertEquals(44, res1)

        val res2 = engine.eval("2 + (bindings[\"abc\"] as Int)")
        assertEquals(15, res2)
    }
}
