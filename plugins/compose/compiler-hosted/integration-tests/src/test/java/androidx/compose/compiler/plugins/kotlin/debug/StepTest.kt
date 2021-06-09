/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.debug

import junit.framework.Test
import junit.framework.TestSuite

class StepTest : AbstractDebuggerTest() {

    fun testSteppingIntoIf() {
        collectDebugEvents(
            """
            import androidx.compose.runtime.*
            @Composable
            fun content() {
                var showVar = computeIt()
                if (showVar) {
                    anotherComposable()            
                }
                println()
            }
            fun computeIt(): Boolean = false
            @Composable
            fun anotherComposable() { }
            """.trimIndent()
        ).assertTrace(
            """
            Test.kt:3 content
            Test.kt:4 content
            Test.kt:10 computeIt
            Test.kt:4 content
            Test.kt:5 content
            Test.kt:7 content
            Test.kt:8 content
            Test.kt:9 content
            """.trimIndent()
        )
    }

    companion object {
        @JvmStatic
        fun suite(): Test {
            val testSuite = TestSuite(StepTest::class.java)
            return DebugTestSetup(testSuite) { debugEnv ->
                testSuite.tests().toList().filterIsInstance(AbstractDebuggerTest::class.java)
                    .forEach {
                        it.initialize(debugEnv.virtualMachine, debugEnv.proxyPort)
                    }
            }
        }
    }
}
