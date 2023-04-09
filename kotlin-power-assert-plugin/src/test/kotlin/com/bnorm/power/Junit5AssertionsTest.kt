/*
 * Copyright (C) 2021 Brian Norman
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

import org.jetbrains.kotlin.name.FqName
import kotlin.test.Test

class Junit5AssertionsTest {
  @Test
  fun `test JUnit5 Assertions#assertTrue transformation`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertTrue
      
      fun main() {
        assertTrue(1 != 1)
      }""",
      """
      Assertion failed
      assertTrue(1 != 1)
                   |
                   false ==> expected: <true> but was: <false>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertTrue transformation with message`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertTrue
      
      fun main() {
        assertTrue(1 != 1, "Message:")
      }""",
      """
      Message:
      assertTrue(1 != 1, "Message:")
                   |
                   false ==> expected: <true> but was: <false>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertTrue transformation with local variable message`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertTrue
      
      fun main() {
        val message = "Message:"
        assertTrue(1 != 1, message)
      }""",
      """
      Message:
      assertTrue(1 != 1, message)
                   |
                   false ==> expected: <true> but was: <false>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertTrue transformation with message supplier`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertTrue
      
      fun main() {
        assertTrue(1 != 1) { "Message:" }
      }""",
      """
      Message:
      assertTrue(1 != 1) { "Message:" }
                   |
                   false ==> expected: <true> but was: <false>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertTrue transformation with local variable message supplier`() {
    assertMessage(
      """
      import java.util.function.Supplier
      import org.junit.jupiter.api.Assertions.assertTrue
      
      fun main() {
        val supplier = Supplier { "Message:" }
        assertTrue(1 != 1, supplier)
      }""",
      """
      Message:
      assertTrue(1 != 1, supplier)
                   |
                   false ==> expected: <true> but was: <false>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertFalse transformation`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertFalse
      
      fun main() {
        assertFalse(1 == 1)
      }""",
      """
      Assertion failed
      assertFalse(1 == 1)
                    |
                    true ==> expected: <false> but was: <true>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertFalse transformation with message`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertFalse
      
      fun main() {
        assertFalse(1 == 1, "Message:")
      }""",
      """
      Message:
      assertFalse(1 == 1, "Message:")
                    |
                    true ==> expected: <false> but was: <true>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertFalse transformation with local variable message`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertFalse
      
      fun main() {
        val message = "Message:"
        assertFalse(1 == 1, message)
      }""",
      """
      Message:
      assertFalse(1 == 1, message)
                    |
                    true ==> expected: <false> but was: <true>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertFalse transformation with message supplier`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertFalse
      
      fun main() {
        assertFalse(1 == 1) { "Message:" }
      }""",
      """
      Message:
      assertFalse(1 == 1) { "Message:" }
                    |
                    true ==> expected: <false> but was: <true>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertFalse transformation with local variable message supplier`() {
    assertMessage(
      """
      import java.util.function.Supplier
      import org.junit.jupiter.api.Assertions.assertFalse
      
      fun main() {
        val supplier = Supplier { "Message:" }
        assertFalse(1 == 1, supplier)
      }""",
      """
      Message:
      assertFalse(1 == 1, supplier)
                    |
                    true ==> expected: <false> but was: <true>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertEquals transformation`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertEquals
      
      fun main() {
        val greeting = "Hello"
        val name = "World"
        assertEquals(greeting, name)
      }""",
      """
      assertEquals(greeting, name)
                   |         |
                   |         World
                   Hello ==> expected: <Hello> but was: <World>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertEquals transformation with message`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertEquals
      
      fun main() {
        val greeting = "Hello"
        val name = "World"
        assertEquals(greeting, name, "Message:")
      }""",
      """
      Message:
      assertEquals(greeting, name, "Message:")
                   |         |
                   |         World
                   Hello ==> expected: <Hello> but was: <World>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertEquals transformation with local variable message`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertEquals
      
      fun main() {
        val greeting = "Hello"
        val name = "World"
        val message = "Message:"
        assertEquals(greeting, name, message)
      }""",
      """
      Message:
      assertEquals(greeting, name, message)
                   |         |
                   |         World
                   Hello ==> expected: <Hello> but was: <World>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertEquals transformation with message supplier`() {
    assertMessage(
      """
      import org.junit.jupiter.api.Assertions.assertEquals
      
      fun main() {
        val greeting = "Hello"
        val name = "World"
        assertEquals(greeting, name) { "Message:" }
      }""",
      """
      Message:
      assertEquals(greeting, name) { "Message:" }
                   |         |
                   |         World
                   Hello ==> expected: <Hello> but was: <World>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals"))),
    )
  }

  @Test
  fun `test JUnit5 Assertions#assertEquals transformation with local variable message supplier`() {
    assertMessage(
      """
      import java.util.function.Supplier
      import org.junit.jupiter.api.Assertions.assertEquals
      
      fun main() {
        val greeting = "Hello"
        val name = "World"
        val supplier = Supplier { "Message:" }
        assertEquals(greeting, name, supplier)
      }""",
      """
      Message:
      assertEquals(greeting, name, supplier)
                   |         |
                   |         World
                   Hello ==> expected: <Hello> but was: <World>
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals"))),
    )
  }
}
