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
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.name.FqName
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
  fun booleanMixAndLast() {
    assertMessage(
      """
fun main() {
  val text = "Hello"
  assert((text.length == 1 || text.toLowerCase() == text) && text.length == 1)
}""",
      """
Assertion failed
assert((text.length == 1 || text.toLowerCase() == text) && text.length == 1)
        |    |      |       |    |             |  |
        |    |      |       |    |             |  Hello
        |    |      |       |    |             false
        |    |      |       |    hello
        |    |      |       Hello
        |    |      false
        |    5
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
  fun booleanMixOrLast() {
    assertMessage(
      """
fun main() {
  val text = "Hello"
  assert((text.length == 5 && text.toLowerCase() == text) || text.length == 1)
}""",
      """
Assertion failed
assert((text.length == 5 && text.toLowerCase() == text) || text.length == 1)
        |    |      |       |    |             |  |        |    |      |
        |    |      |       |    |             |  |        |    |      false
        |    |      |       |    |             |  |        |    5
        |    |      |       |    |             |  |        Hello
        |    |      |       |    |             |  Hello
        |    |      |       |    |             false
        |    |      |       |    hello
        |    |      |       Hello
        |    |      true
        |    5
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

  @Test
  fun infixFunctions() {
    assertMessage(
      """
fun main() {
  assert(1.shl(1) == 4)
}""",
      """
Assertion failed
assert(1.shl(1) == 4)
         |      |
         |      false
         2
""".trimIndent()
    )

    assertMessage(
      """
fun main() {
  assert(1 shl 1 == 4)
}""",
      """
Assertion failed
assert(1 shl 1 == 4)
         |     |
         |     false
         2
""".trimIndent()
    )
  }

  @Test
  fun multiline() {
    assertMessage(
      """fun main() {
  val text: String? = "Hello"
  assert(
    text
        == null ||
        (
            text.length == 5 &&
                text.toLowerCase() == text
            )
  )
}""",
      """
Assertion failed
assert(
  text
  |
  Hello
      == null ||
      |
      false
      (
          text.length == 5 &&
          |    |      |
          |    |      true
          |    5
          Hello
              text.toLowerCase() == text
              |    |             |  |
              |    |             |  Hello
              |    |             false
              |    hello
              Hello
          )
)
""".trimIndent()
    )
  }

  @Test
  fun assertTrueCustomMessage() {
    assertMessage(
      """
import kotlin.test.assertTrue

fun main() {
  val text: String? = "Hello"
  assertTrue(1 == 2, message = "${"$"}text, the world is broken")
}""",
      """
Hello, the world is broken
assertTrue(1 == 2, message = "${"$"}text, the world is broken")
             |
             false
""".trimIndent(),
      PowerAssertComponentRegistrar(setOf(FqName("kotlin.test.assertTrue")))
    )
  }

  @Test
  fun requireCustomMessage() {
    assertMessage(
      """
fun main() {
  require(1 == 2) { "the world is broken" }
}""",
      """
the world is broken
require(1 == 2) { "the world is broken" }
          |
          false
""".trimIndent(),
      PowerAssertComponentRegistrar(setOf(FqName("kotlin.require")))
    )
  }

  @Test
  fun checkCustomMessage() {
    assertMessage(
      """
fun main() {
  check(1 == 2) { "the world is broken" }
}""",
      """
the world is broken
check(1 == 2) { "the world is broken" }
        |
        false
""".trimIndent(),
      PowerAssertComponentRegistrar(setOf(FqName("kotlin.check")))
    )
  }

  @Test
  fun carriageReturnRemoval() {
    assertMessage(
      """
fun main() {
  val a = 0
  assert(a == 42)
}""".replace("\n", "\r\n"),
      """
Assertion failed
assert(a == 42)
       | |
       | false
       0
""".trimIndent())
  }

  @Test
  fun constantExpression() {
    assertMessage(
      """
fun main() {
  assert(true)
  assert(false)
}""",
      """
Assertion failed
""".trimIndent()
    )
  }
}
