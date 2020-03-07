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
import kotlin.test.assertTrue

data class Person(
  val firstName: String,
  val lastName: String
)

class PowerAssertTest {
  private val people = listOf(Person("John", "Doe"), Person("Jane", "Doe"))

  @Test
  fun assertTrue() {
    assertTrue(people.size == 1)
  }

  @Test
  fun assert() {
    assert(people.size == 1)
  }

  @Test
  fun require() {
    require(people.size == 1)
  }
}
