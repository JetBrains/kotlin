/*
 * Copyright (C) 2020 Brian Norman
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

package com.bnorm.power

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.fail

class CompilerTest {
  @Test
  fun testMyCompilerPlugin() {
    assertMessage(
      """
fun main() {
  val hello = "Brian"
  assert(hello == "World")
}""",
      """
Assertion failed:
assert(hello == "World")
       |
       false
""".trimIndent()
    )
  }

}

fun assertMessage(@Language("kotlin") source: String, message: String) {
  val result = KotlinCompilation().apply {
    sources = listOf(SourceFile.kotlin("main.kt", source))
    useIR = true
    messageOutputStream = System.out
    compilerPlugins = listOf(PowerAssertComponentRegistrar())
    inheritClassPath = true
  }.compile()

  assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

  val kClazz = result.classLoader.loadClass("MainKt")
  val main = kClazz.declaredMethods.single { it.name == "main" }
  try {
    try {
      main.invoke(null)
    } catch (t: InvocationTargetException) {
      throw t.cause!!
    }
    fail("should have thrown assertion")
  } catch (t: AssertionError) {
    assertEquals(message, t.message)
  }
}
