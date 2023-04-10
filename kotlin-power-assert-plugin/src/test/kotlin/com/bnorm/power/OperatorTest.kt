/*
 * Copyright (C) 2023 Brian Norman
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

class OperatorTest {
  @Test
  fun `contains operator is correctly aligned`() {
    assertMessage(
      """
      fun main() {
        assert("Name" in listOf("Hello", "World"))
      }""",
      """
      Assertion failed
      assert("Name" in listOf("Hello", "World"))
                    |  |
                    |  [Hello, World]
                    false
      """.trimIndent(),
    )
  }

  @Test
  fun `contains function is correctly aligned`() {
    assertMessage(
      """
      fun main() {
        assert(listOf("Hello", "World").contains("Name"))
      }""",
      """
      Assertion failed
      assert(listOf("Hello", "World").contains("Name"))
             |                        |
             |                        false
             [Hello, World]
      """.trimIndent(),
    )
  }

  @Test
  fun `negative contains operator is correctly aligned`() {
    assertMessage(
      """
      fun main() {
        assert("Hello" !in listOf("Hello", "World"))
      }""",
      """
      Assertion failed
      assert("Hello" !in listOf("Hello", "World"))
                     |   |
                     |   [Hello, World]
                     false
      """.trimIndent(),
    )
  }

  @Test
  fun `negative contains function is correctly aligned`() {
    assertMessage(
      """
      fun main() {
        assert(!listOf("Hello", "World").contains("Hello"))
      }""",
      """
      Assertion failed
      assert(!listOf("Hello", "World").contains("Hello"))
             ||                        |
             ||                        true
             |[Hello, World]
             false
      """.trimIndent(),
    )
  }
}
