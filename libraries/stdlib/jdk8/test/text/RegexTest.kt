/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.text.test

import org.junit.Test
import kotlin.test.*

class RegexTest {
    @Test fun namedGroups() {
        val input = "1a 2b 3c"
        val regex = "(?<num>\\d)(?<liter>\\w)".toRegex()

        val matches = regex.findAll(input).toList()
        assertTrue(matches.all { it.groups.size == 3 })
        val m1 = matches[0]

        assertEquals("1", m1.groups["num"]?.value)
        assertEquals(0..0, m1.groups["num"]?.range)
        assertEquals("a", m1.groups["liter"]?.value)
        assertEquals(1..1, m1.groups["liter"]?.range)

        val m2 = matches[1]
        assertEquals("2", m2.groups["num"]?.value)
        assertEquals(3..3, m2.groups["num"]?.range)
        assertEquals("b", m2.groups["liter"]?.value)
        assertEquals(4..4, m2.groups["liter"]?.range)
    }
}
