/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.junit.Test
import java.io.StringWriter
import javax.script.ScriptEngineManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TestEvalOnlyReplEngine {

    @Test
    fun testJsr223BasicEvalOnlyEngine() {
        val factory = ScriptEngineManager()
        val engine = factory.getEngineByName(EvalOnlyJsr223ReplEngineFactory.jsr223EngineName)

        val capture = StringWriter()
        engine.context.writer = capture

        engine.put("z", 33)

        engine.eval("""println("Hello keplin-kotin-eval-only engine")""")

        engine.eval("""val x = 10 + context.getAttribute("z") as Int""")
        engine.eval("""println(x)""")
        val result = engine.eval("""x + 20""")
        assertEquals(63, result)

        val checkEngine = engine.eval("""kotlinScript != null""") as Boolean
        assertTrue(checkEngine)
        val result2 = engine.eval("""x + context.getAttribute("boundValue") as Int""", engine.createBindings().apply {
            put("boundValue", 100)
        })
        assertEquals(143, result2)

        assertEquals("Hello keplin-kotin-eval-only engine\n43\n", capture.toString())
    }

}