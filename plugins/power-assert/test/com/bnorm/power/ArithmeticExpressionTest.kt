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

import kotlin.test.Test
import kotlin.test.assertEquals

class ArithmeticExpressionTest {
  @Test
  fun `assertion with inline addition`() {
    val actual = executeMainAssertion("assert(1 + 1 == 4)")
    assertEquals(
      """
      Assertion failed
      assert(1 + 1 == 4)
               |   |
               |   false
               2
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline subtraction`() {
    val actual = executeMainAssertion("assert(3 - 1 == 4)")
    assertEquals(
      """
      Assertion failed
      assert(3 - 1 == 4)
               |   |
               |   false
               2
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline multiplication`() {
    val actual = executeMainAssertion("assert(1 * 2 == 4)")
    assertEquals(
      """
      Assertion failed
      assert(1 * 2 == 4)
               |   |
               |   false
               2
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline division`() {
    val actual = executeMainAssertion("assert(2 / 1 == 4)")
    assertEquals(
      """
      Assertion failed
      assert(2 / 1 == 4)
               |   |
               |   false
               2
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline prefix increment`() {
    val actual = executeMainAssertion(
      """
      var i = 1
      assert(++i == 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      assert(++i == 4)
             |   |
             |   false
             2
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline postfix increment`() {
    val actual = executeMainAssertion(
      """
      var i = 1
      assert(i++ == 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      assert(i++ == 4)
             |   |
             |   false
             1
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline prefix decrement`() {
    val actual = executeMainAssertion(
      """
      var i = 3
      assert(--i == 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      assert(--i == 4)
             |   |
             |   false
             2
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `assertion with inline postfix decrement`() {
    val actual = executeMainAssertion(
      """
      var i = 3
      assert(i-- == 4)
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      assert(i-- == 4)
             |   |
             |   false
             3
      """.trimIndent(),
      actual,
    )
  }
}
