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

import kotlin.test.Test
import org.jetbrains.kotlin.name.FqName

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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertTrue")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertFalse")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals")))
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
      PowerAssertComponentRegistrar(setOf(FqName("org.junit.jupiter.api.Assertions.assertEquals")))
    )
  }
}
