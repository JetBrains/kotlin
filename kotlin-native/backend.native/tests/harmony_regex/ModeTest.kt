/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class ModeTest {

    /**
     * Tests Pattern compilation modes and modes triggered in pattern strings
     */
    @Test fun testCase() {
        var regex: Regex
        var result: MatchResult?

        regex = Regex("([a-z]+)[0-9]+")
        result = regex.find("cAT123#dog345")
        assertNotNull(result)
        assertEquals("dog", result!!.groupValues[1])
        assertNull(result.next())

        regex = Regex("([a-z]+)[0-9]+", RegexOption.IGNORE_CASE)
        result = regex.find("cAt123#doG345")
        assertNotNull(result)
        assertEquals("cAt", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("doG", result!!.groupValues[1])
        assertNull(result.next())

        regex = Regex("(?i)([a-z]+)[0-9]+")
        result = regex.find("cAt123#doG345")
        assertNotNull(result)
        assertEquals("cAt", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("doG", result!!.groupValues[1])
        assertNull(result.next())
    }

    @Test fun testMultiline() {
        var regex: Regex
        var result: MatchResult?

        regex = Regex("^foo")
        result = regex.find("foobar")
        assertNotNull(result)
        assertTrue(result!!.range.start == 0 && result.range.endInclusive == 2)
        assertTrue(result.groups[0]!!.range.start == 0 && result.groups[0]!!.range.endInclusive == 2)
        assertNull(result.next())

        result = regex.find("barfoo")
        assertNull(result)

        regex = Regex("foo$")
        result = regex.find("foobar")
        assertNull(result)

        result = regex.find("barfoo")
        assertNotNull(result)
        assertTrue(result!!.range.start == 3 && result.range.endInclusive == 5)
        assertTrue(result.groups[0]!!.range.start == 3 && result.groups[0]!!.range.endInclusive == 5)
        assertNull(result.next())

        regex = Regex("^foo([0-9]*)", RegexOption.MULTILINE)
        result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
        assertNotNull(result)
        assertEquals("1", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("2", result!!.groupValues[1])
        assertNull(result.next())

        regex = Regex("foo([0-9]*)$", RegexOption.MULTILINE)
        result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
        assertNotNull(result)
        assertEquals("3", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("4", result!!.groupValues[1])
        assertNull(result.next())

        regex = Regex("(?m)^foo([0-9]*)")
        result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
        assertNotNull(result)
        assertEquals("1", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("2", result!!.groupValues[1])
        assertNull(result.next())

        regex = Regex("(?m)foo([0-9]*)$")
        result = regex.find("foo1bar\nfoo2foo3\nbarfoo4")
        assertNotNull(result)
        assertEquals("3", result!!.groupValues[1])
        result = result.next()
        assertNotNull(result)
        assertEquals("4", result!!.groupValues[1])
        assertNull(result.next())
    }

}
