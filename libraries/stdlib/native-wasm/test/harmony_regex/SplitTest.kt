/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class SplitTest {

    @Test fun testSimple() {
        val p = Regex("/")
        val results = p.split("have/you/done/it/right")
        val expected = arrayOf("have", "you", "done", "it", "right")
        assertEquals(expected.size, results.size)
        for (i in expected.indices) {
            assertEquals(results[i], expected[i])
        }
    }

    @Test fun testSplit1() {
        var p = Regex(" ")

        val input = "poodle zoo"
        var tokens: List<String>

        tokens = p.split(input, 1)
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] == input)
        tokens = p.split(input, 2)
        assertEquals(2, tokens.size)
        assertEquals("poodle", tokens[0])
        assertEquals("zoo", tokens[1])
        tokens = p.split(input, 5)
        assertEquals(2, tokens.size)
        assertEquals("poodle", tokens[0])
        assertEquals("zoo", tokens[1])
        tokens = p.split(input, 0)
        assertEquals(2, tokens.size)
        assertEquals("poodle", tokens[0])
        assertEquals("zoo", tokens[1])
        tokens = p.split(input)
        assertEquals(2, tokens.size)
        assertEquals("poodle", tokens[0])
        assertEquals("zoo", tokens[1])

        p = Regex("d")

        tokens = p.split(input, 1)
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] == input)
        tokens = p.split(input, 2)
        assertEquals(2, tokens.size)
        assertEquals("poo", tokens[0])
        assertEquals("le zoo", tokens[1])
        tokens = p.split(input, 5)
        assertEquals(2, tokens.size)
        assertEquals("poo", tokens[0])
        assertEquals("le zoo", tokens[1])
        tokens = p.split(input, 0)
        assertEquals(2, tokens.size)
        assertEquals("poo", tokens[0])
        assertEquals("le zoo", tokens[1])
        tokens = p.split(input)
        assertEquals(2, tokens.size)
        assertEquals("poo", tokens[0])
        assertEquals("le zoo", tokens[1])

        p = Regex("o")

        tokens = p.split(input, 1)
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] == input)
        tokens = p.split(input, 2)
        assertEquals(2, tokens.size)
        assertEquals("p", tokens[0])
        assertEquals("odle zoo", tokens[1])
        tokens = p.split(input, 5)
        assertEquals(5, tokens.size)
        assertEquals("p", tokens[0])
        assertTrue(tokens[1] == "")
        assertEquals("dle z", tokens[2])
        assertTrue(tokens[3] == "")
        assertTrue(tokens[4] == "")
        tokens = p.split(input, 0)
        assertEquals(5, tokens.size)
        assertEquals("p", tokens[0])
        assertTrue(tokens[1] == "")
        assertEquals("dle z", tokens[2])
        assertTrue(tokens[3] == "")
        assertTrue(tokens[4] == "")
        tokens = p.split(input)
        assertEquals(5, tokens.size)
        assertEquals("p", tokens[0])
        assertTrue(tokens[1] == "")
        assertEquals("dle z", tokens[2])
        assertTrue(tokens[3] == "")
        assertTrue(tokens[4] == "")
    }

    @Test fun testSplit2() {
        val p = Regex("")
        var s: List<String>
        s = p.split("a", 0)
        assertEquals(3, s.size)
        assertEquals("", s[0])
        assertEquals("a", s[1])
        assertEquals("", s[2])

        s = p.split("", 0)
        assertEquals(2, s.size)
        assertEquals("", s[0])
        assertEquals("", s[1])

        s = p.split("abcd", 0)
        assertEquals(6, s.size)
        assertEquals("", s[0])
        assertEquals("a", s[1])
        assertEquals("b", s[2])
        assertEquals("c", s[3])
        assertEquals("d", s[4])
        assertEquals("", s[5])
    }
}
