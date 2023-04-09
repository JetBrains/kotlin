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

class LamdaTest {
  @Test
  fun `list operations assert`() {
    assertMessage(
      """
      fun main() {
        val list = listOf("Jane", "John")
        assert(list.map { "Doe, ${'$'}it" }.any { it == "Scott, Michael" })
      }""",
      """
      Assertion failed
      assert(list.map { "Doe, ${'$'}it" }.any { it == "Scott, Michael" })
             |    |                  |
             |    |                  false
             |    [Doe, Jane, Doe, John]
             [Jane, John]
      """.trimIndent(),
    )
  }

  @Test
  fun `list operations require`() {
    assertMessage(
      """
      fun main() {
        val list = listOf("Jane", "John")
        require(
            value = list
                .map { "Doe, ${'$'}it" }
                .any { it == "Scott, Michael" }
        )
      }""",
      """
      Assertion failed
      require(
          value = list
                  |
                  [Jane, John]
              .map { "Doe, ${'$'}it" }
               |
               [Doe, Jane, Doe, John]
              .any { it == "Scott, Michael" }
               |
               false
      )
      """.trimIndent(),
      PowerAssertCompilerPluginRegistrar(setOf(FqName("kotlin.require"))),
    )
  }
}
