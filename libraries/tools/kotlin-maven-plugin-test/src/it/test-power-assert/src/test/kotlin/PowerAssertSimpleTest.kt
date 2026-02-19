/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.fail

class PowerAssertSimpleTest {
    @Test
    fun assertTest() {
        val error = try {
            assert(Person.UNKNOWN.size == 1) { "unknown persons: ${Person.UNKNOWN}" }
            fail("Expected assert to fail!")
            return
        } catch (e: AssertionError) {
            e
        }

        assertEquals(
            """
                unknown persons: [Person(firstName=John, lastName=Doe), Person(firstName=Jane, lastName=Doe)]
                assert(Person.UNKNOWN.size == 1) { "unknown persons: ${"$"}{Person.UNKNOWN}" }
                              |       |    |
                              |       |    false
                              |       2
                              [Person(firstName=John, lastName=Doe), Person(firstName=Jane, lastName=Doe)]
                
            """.trimIndent(),
            error.message,
        )
    }
}
