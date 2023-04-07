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

import kotlin.test.Test
import kotlin.test.assertEquals

class CastExpressionTest {
  @Test
  fun `instance check is correctly aligned`() {
    val actual = executeMainAssertion("""assert(null is String)""")
    assertEquals(
      """
      Assertion failed
      assert(null is String)
                  |
                  false
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `negative instance check is correctly aligned`() {
    val actual = executeMainAssertion("""assert("Hello, world!" !is String)""")
    assertEquals(
      """
      Assertion failed
      assert("Hello, world!" !is String)
                             |
                             false
      """.trimIndent(),
      actual,
    )
  }

  @Test
  fun `smart casts do not duplicate output`() {
    val actual = executeMainAssertion(
      """
      val greeting: Any = "hello"
      assert(greeting is String && greeting.length == 2)
      """.trimIndent(),
    )
    assertEquals(
      """
      Assertion failed
      assert(greeting is String && greeting.length == 2)
             |        |            |        |      |
             |        |            |        |      false
             |        |            |        5
             |        |            hello
             |        true
             hello
      """.trimIndent(),
      actual,
    )
  }
}
