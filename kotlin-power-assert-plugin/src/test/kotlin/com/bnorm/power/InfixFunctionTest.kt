/*
 * Copyright (C) 2022 Brian Norman
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
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class InfixFunctionTest {
  @Test
  fun `extension infix function call includes receiver`() {
    val actual = runExtensionInfix(
      """
      (1 + 1) mustEqual (2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      (1 + 1) mustEqual (2 + 4)
         |                 |
         |                 6
         2
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension infix function call with constant receiver`() {
    val actual = runExtensionInfix(
      """
      1 mustEqual (2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      1 mustEqual (2 + 4)
                     |
                     6
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension infix function call with constant parameter`() {
    val actual = runExtensionInfix(
      """
      (1 + 1) mustEqual 6
      """.trimIndent(),
    )
    assertEquals(
      """
      (1 + 1) mustEqual 6
         |
         2
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension infix function call with only constants`() {
    val actual = runExtensionInfix(
      """
      2 mustEqual 6
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension non-infix function call includes receiver`() {
    val actual = runExtensionInfix(
      """
      (1 + 1).mustEqual(2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      (1 + 1).mustEqual(2 + 4)
         |                |
         |                6
         2
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension non-infix function call with constant receiver`() {
    val actual = runExtensionInfix(
      """
      1.mustEqual(2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      1.mustEqual(2 + 4)
                    |
                    6
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension non-infix function call with constant parameter`() {
    val actual = runExtensionInfix(
      """
      (1 + 1).mustEqual(6)
      """.trimIndent(),
    )
    assertEquals(
      """
      (1 + 1).mustEqual(6)
         |
         2
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `extension non-infix function call with only constants`() {
    val actual = runExtensionInfix(
      """
      2.mustEqual(6)
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch infix function call includes receiver`() {
    val actual = runDispatchInfix(
      """
      Wrapper(1 + 1) mustEqual (2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(1 + 1) mustEqual (2 + 4)
      |         |                 |
      |         |                 6
      |         2
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch infix function call with constant receiver`() {
    val actual = runDispatchInfix(
      """
      Wrapper(1) mustEqual (2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(1) mustEqual (2 + 4)
      |                       |
      |                       6
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch infix function call with constant parameter`() {
    val actual = runDispatchInfix(
      """
      Wrapper(1 + 1) mustEqual 6
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(1 + 1) mustEqual 6
      |         |
      |         2
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch infix function call with only constants`() {
    val actual = runDispatchInfix(
      """
      Wrapper(2) mustEqual 6
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(2) mustEqual 6
      |
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch non-infix function call includes receiver`() {
    val actual = runDispatchInfix(
      """
      Wrapper(1 + 1).mustEqual(2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(1 + 1).mustEqual(2 + 4)
      |         |                |
      |         |                6
      |         2
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch non-infix function call with constant receiver`() {
    val actual = runDispatchInfix(
      """
      Wrapper(1).mustEqual(2 + 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(1).mustEqual(2 + 4)
      |                      |
      |                      6
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch non-infix function call with constant parameter`() {
    val actual = runDispatchInfix(
      """
      Wrapper(1 + 1).mustEqual(6)
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(1 + 1).mustEqual(6)
      |         |
      |         2
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `dispatch non-infix function call with only constants`() {
    val actual = runDispatchInfix(
      """
      Wrapper(2).mustEqual(6)
      """.trimIndent(),
    )
    assertEquals(
      """
      Wrapper(2).mustEqual(6)
      |
      Wrapper
      """.trimIndent(),
      actual.trim(),
    )
  }

  private fun runExtensionInfix(mainBody: String): String {
    return run(
      SourceFile.kotlin(
        name = "main.kt",
        contents = """
        infix fun <V> V.mustEqual(expected: V): Unit = assert(this == expected)
        
        fun <V> V.mustEqual(expected: V, message: () -> String): Unit =
          assert(this == expected, message)
        
        fun main() {
          $mainBody
        }
        """.trimIndent(),
        trimIndent = false,
      ),
      setOf(FqName("mustEqual")),
    )
  }

  private fun runDispatchInfix(mainBody: String): String {
    return run(
      SourceFile.kotlin(
        name = "main.kt",
        contents = """
        class Wrapper<V>(
          private val value: V
        ) {
          infix fun mustEqual(expected: V): Unit = assert(value == expected)
        
          fun mustEqual(expected: V, message: () -> String): Unit =
            assert(value == expected, message)
        
          override fun toString() = "Wrapper"
        }
        
        fun main() {
          $mainBody
        }
        """.trimIndent(),
        trimIndent = false,
      ),
      setOf(FqName("Wrapper.mustEqual")),
    )
  }

  private fun run(file: SourceFile, fqNames: Set<FqName>, main: String = "MainKt"): String {
    val result = compile(listOf(file), PowerAssertCompilerPluginRegistrar(fqNames))
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, "Failed with messages: " + result.messages)

    val kClazz = result.classLoader.loadClass(main)
    val mainMethod = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
    try {
      try {
        mainMethod.invoke(null)
      } catch (t: InvocationTargetException) {
        throw t.cause!!
      }
      fail("should have thrown assertion")
    } catch (t: Throwable) {
      return t.message ?: ""
    }
  }
}
