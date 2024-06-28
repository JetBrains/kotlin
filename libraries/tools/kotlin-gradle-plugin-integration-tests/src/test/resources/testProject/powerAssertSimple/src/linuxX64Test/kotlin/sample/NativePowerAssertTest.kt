/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativePowerAssertTest {
    @Test
    fun assert() {
        val error = assertFailsWith<AssertionError> {
            assert(Person.UNKNOWN.size == 1) { "unknown persons: ${Person.UNKNOWN}" }
        }
        assertEquals(
            actual = error.message,
            expected = """
                unknown persons: [Person(firstName=John, lastName=Doe), Person(firstName=Jane, lastName=Doe)]
                assert(Person.UNKNOWN.size == 1) { "unknown persons: ${"$"}{Person.UNKNOWN}" }
                       |      |       |    |
                       |      |       |    false
                       |      |       2
                       |      [Person(firstName=John, lastName=Doe), Person(firstName=Jane, lastName=Doe)]
                       Person.Companion
                
            """.trimIndent(),
        )
    }
}
