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

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PowerAssertTest {

    @AfterTest
    fun cleanup() {
        debugLog.clear()
    }

    @Test
    fun assertTrue() {
        val error = assertFailsWith<AssertionError> { assertTrue(Person.UNKNOWN.size == 1) }
        assertEquals(
            actual = error.message,
            expected = """
                
                assertTrue(Person.UNKNOWN.size == 1)
                           |      |       |    |
                           |      |       |    false
                           |      |       2
                           |      [Person(firstName=John, lastName=Doe), Person(firstName=Jane, lastName=Doe)]
                           Person.Companion
                
            """.trimIndent()
        )
    }

    @Test
    fun require() {
        val error = assertFailsWith<IllegalArgumentException> { require(Person.UNKNOWN.size == 1) }
        assertEquals(
            actual = error.message,
            expected = """
                
                require(Person.UNKNOWN.size == 1)
                        |      |       |    |
                        |      |       |    false
                        |      |       2
                        |      [Person(firstName=John, lastName=Doe), Person(firstName=Jane, lastName=Doe)]
                        Person.Companion
                
            """.trimIndent()
        )
    }

    @Test
    fun excludedRequire() {
        val error = assertFailsWith<IllegalArgumentException> { Person("", "") }
        assertEquals(
            actual = error.message,
            expected = "Failed requirement."
        )
    }

    @Test
    fun softAssert() {
        val unknown: List<Person>? = Person.UNKNOWN
        assert(unknown != null)
        assert(unknown.size == 2)

        val error = assertFailsWith<AssertionError> {
            val jane: Person
            val john: Person
            assertSoftly {
                jane = unknown[0]
                assert(jane.firstName == "Jane")
                assert(jane.lastName == "Doe") { "bad jane last name" }

                john = unknown[1]
                assert(john.lastName == "Doe" && john.firstName == "John") { "bad john" }
            }
        }
        assertEquals(
            actual = error.message,
            expected = """
                Multiple failed assertions
            """.trimIndent(),
        )
        assertEquals(
            actual = error.suppressedExceptions.size,
            expected = 2,
        )
        assertEquals(
            actual = error.suppressedExceptions[0].message,
            expected = """
                
                assert(jane.firstName == "Jane")
                       |    |         |
                       |    |         false
                       |    John
                       Person(firstName=John, lastName=Doe)
                
            """.trimIndent(),
        )
        assertEquals(
            actual = error.suppressedExceptions[1].message,
            expected = """
                bad john
                assert(john.lastName == "Doe" && john.firstName == "John") { "bad john" }
                       |    |        |           |    |         |
                       |    |        |           |    |         false
                       |    |        |           |    Jane
                       |    |        |           Person(firstName=Jane, lastName=Doe)
                       |    |        true
                       |    Doe
                       Person(firstName=Jane, lastName=Doe)
                
            """.trimIndent(),
        )
    }

    @Test
    fun dbgTest() {
        val name = "Jane"
        val greeting = dbg("Hello, $name")
        assert(greeting == "Hello, Jane")
        assertEquals(
            actual = debugLog.toString().trim(),
            expected = """
                dbg("Hello, ${"$"}name")
                    |        |
                    |        Jane
                    Hello, Jane
            """.trimIndent()
        )
    }

    @Test
    fun dbgMessageTest() {
        val name = "Jane"
        val greeting = dbg("Hello, $name", "Greeting:")
        assert(greeting == "Hello, Jane")
        assertEquals(
            actual = debugLog.toString().trim(),
            expected = """
                Greeting:
                dbg("Hello, ${"$"}name", "Greeting:")
                    |        |
                    |        Jane
                    Hello, Jane
            """.trimIndent()
        )
    }
}
