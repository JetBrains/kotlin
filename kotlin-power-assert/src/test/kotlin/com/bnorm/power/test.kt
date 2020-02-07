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
  fun memberFunctions() {
    assertMessage(
      """
fun main() {
  val hello = "Hello"
  assert(hello.length == "World".substring(1, 4).length)
}""",
      """
Assertion failed
assert(hello.length == "World".substring(1, 4).length)
       |     |      |          |               |
       |     |      |          |               3
       |     |      |          orl
       |     |      false
       |     5
       Hello
""".trimIndent()
    )
  }

  @Test
  fun transformations() {
    assertMessage(
      """
fun main() {
  val hello = listOf("Hello", "World")
  assert(hello.reversed() == emptyList<String>())
}""",
      """
Assertion failed
assert(hello.reversed() == emptyList<String>())
       |     |          |  |
       |     |          |  []
       |     |          false
       |     [World, Hello]
       [Hello, World]
""".trimIndent()
    )
  }

  @Test
  fun customMessage() {
    assertMessage(
      """
fun main() {
  assert(1 == 2) { "Not equal" }
}""",
      """
Not equal
assert(1 == 2) { "Not equal" }
         |
         false
""".trimIndent()
    )
  }

  @Test
  fun booleanExpressionsShortCircuit() {
    assertMessage(
      """
fun main() {
  val text: String? = null
  assert(text != null && text.length == 1)
}""",
      """
Assertion failed
assert(text != null && text.length == 1)
       |    |
       |    false
       null
""".trimIndent()
    )
  }

  @Test
  fun booleanAnd() {
    assertMessage(
      """
fun main() {
  val text: String? = "Hello"
  assert(text != null && text.length == 5 && text.toLowerCase() == text)
}""",
      """
Assertion failed
assert(text != null && text.length == 5 && text.toLowerCase() == text)
       |    |          |    |      |       |    |             |  |
       |    |          |    |      |       |    |             |  Hello
       |    |          |    |      |       |    |             false
       |    |          |    |      |       |    hello
       |    |          |    |      |       Hello
       |    |          |    |      true
       |    |          |    5
       |    |          Hello
       |    true
       Hello
""".trimIndent()
    )
  }

  @Test
  fun booleanOr() {
    assertMessage(
      """
fun main() {
  val text: String? = "Hello"
  assert(text == null || text.length == 1 || text.toLowerCase() == text)
}""",
      """
Assertion failed
assert(text == null || text.length == 1 || text.toLowerCase() == text)
       |    |          |    |      |       |    |             |  |
       |    |          |    |      |       |    |             |  Hello
       |    |          |    |      |       |    |             false
       |    |          |    |      |       |    hello
       |    |          |    |      |       Hello
       |    |          |    |      false
       |    |          |    5
       |    |          Hello
       |    false
       Hello
""".trimIndent()
    )
  }

  @Test
  fun booleanMixAndFirst() {
    assertMessage(
      """
fun main() {
  val text: String? = "Hello"
  assert(text != null && (text.length == 1 || text.toLowerCase() == text))
}""",
      """
Assertion failed
assert(text != null && (text.length == 1 || text.toLowerCase() == text))
       |    |           |    |      |       |    |             |  |
       |    |           |    |      |       |    |             |  Hello
       |    |           |    |      |       |    |             false
       |    |           |    |      |       |    hello
       |    |           |    |      |       Hello
       |    |           |    |      false
       |    |           |    5
       |    |           Hello
       |    true
       Hello
""".trimIndent()
    )
  }

  @Test
  fun booleanMixOrFirst() {
    assertMessage(
      """
fun main() {
  val text: String? = "Hello"
  assert(text == null || (text.length == 5 && text.toLowerCase() == text))
}""",
      """
Assertion failed
assert(text == null || (text.length == 5 && text.toLowerCase() == text))
       |    |           |    |      |       |    |             |  |
       |    |           |    |      |       |    |             |  Hello
       |    |           |    |      |       |    |             false
       |    |           |    |      |       |    hello
       |    |           |    |      |       Hello
       |    |           |    |      true
       |    |           |    5
       |    |           Hello
       |    false
       Hello
""".trimIndent()
    )
  }

  @Test
  fun conditionalAccess() {
    assertMessage(
      """
fun main() {
  val text: String? = "Hello"
  assert(text?.length?.minus(2) == 1)
}""",
      """
Assertion failed
assert(text?.length?.minus(2) == 1)
       |     |       |        |
       |     |       |        false
       |     |       3
       |     5
       Hello
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
