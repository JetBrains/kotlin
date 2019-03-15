/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

        val res1 = engine.eval("2 + (bindings[\"abc\"] as Int)")
        assertEquals(15, res1)
    }
}
