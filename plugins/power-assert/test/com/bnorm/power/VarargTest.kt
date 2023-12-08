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

class VarargTest {
  @Test
  fun `implicit array of vararg parameters is excluded from diagram`() {
    assertMessage(
      """
      fun main() {
        var i = 0
        assert(listOf("a", "b", "c") == listOf(i++, i++, i++))
      }""",
      """
      Assertion failed
      assert(listOf("a", "b", "c") == listOf(i++, i++, i++))
             |                     |  |      |    |    |
             |                     |  |      |    |    2
             |                     |  |      |    1
             |                     |  |      0
             |                     |  [0, 1, 2]
             |                     false
             [a, b, c]
      """.trimIndent(),
    )
  }
}
