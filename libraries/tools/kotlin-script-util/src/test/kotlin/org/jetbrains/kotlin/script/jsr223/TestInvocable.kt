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
import javax.script.Invocable
import javax.script.ScriptEngineManager
import kotlin.test.assertEquals


class TestInvocable {
    val engineNames = listOf(EvalOnlyJsr223ReplEngineFactory.jsr223EngineName, CompilableJsr223ReplEngineFactory.jsr223EngineName)

    @Test
    fun testJsr223InvocableFunctionAndMethod() {
        val factory = ScriptEngineManager()
        engineNames.forEach { engineName ->
            val engine = factory.getEngineByName(engineName)
            val invoker = engine as Invocable

            val capture = StringWriter()
            engine.context.writer = capture

            engine.eval("""fun foo(x: Int) = x + 20""")
            assertEquals(50, invoker.invokeFunction("foo", 30))

            engine.eval("""class Bar(val base: Int) { fun foo(x: Int) = x + base; }""")
            val barInstance = engine.eval("""Bar(50)""")
            assertEquals(75, engine.invokeMethod(barInstance, "foo", 25))

            // and call old thing again
            assertEquals(50, invoker.invokeFunction("foo", 30))
        }
    }

    @Test
    fun testJsr223InvocableInterface() {
        val factory = ScriptEngineManager()
        engineNames.forEach { engineName ->
            val engine = factory.getEngineByName(engineName)
            val invoker = engine as Invocable

            val capture = StringWriter()
            engine.context.writer = capture

            engine.eval("""fun run() { println("running") }""")
            invoker.getInterface(Runnable::class.java).run()
            assertEquals("running\n", capture.toString())

            // we do this in three parts to make sure interface can span multiple evals

            engine.eval("""
                      fun one(x: Int, y: Int) = x + y
                    """)
            engine.eval("""
                      fun two(something: String) = "twoes "+something
                    """)
            engine.eval("""
                      fun three(): Int = 10
                    """)
            val foofy = invoker.getInterface(Foofy::class.java)
            assertEquals(33, foofy.one(21, 12))
            assertEquals("twoes a crowd", foofy.two("a crowd"))
            assertEquals(10, foofy.three())

            // go back and do runnable again!
            invoker.getInterface(Runnable::class.java).run()
            assertEquals("running\nrunning\n", capture.toString())
        }
    }

    interface Foofy {
        fun one(x: Int, y: Int): Int
        fun two(s: String): String
        fun three(): Int
    }
}