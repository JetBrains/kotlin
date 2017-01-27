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

package test.js

import kotlin.js.*

import kotlin.test.*
import org.junit.Test as test

class RegExpTest {

    @test fun regExpToString() {
        val pattern = "q(\\d+)d"
        val re = RegExp(pattern, "i")
        assertEquals("/$pattern/i", re.toString())
    }

    @test fun regExpProperties() {
        val re1 = RegExp("[a-z]", "img")
        assertTrue(re1.global)
        assertTrue(re1.ignoreCase)
        assertTrue(re1.multiline)
        val re2 = RegExp("\\d")
        assertFalse(re2.global)
        assertFalse(re2.ignoreCase)
        assertFalse(re2.multiline)
    }

    @test fun regExpTest() {
        val pattern = "q(\\d+)d"
        val re = RegExp(pattern, "i")

        assertTrue(re.test("test q12D string"))
        assertFalse(re.test("sample"))

        assertFalse(RegExp("\\w").test("?"))
    }


    @test fun regExpExec() {
        val string = "R2D2 beats A5D5 "
        var re = RegExp("""(\w\d)(\w\d)""", "g")
        val m1 = re.exec(string)!!
        assertEquals(listOf("R2D2", "R2", "D2"), m1.asArray().asList())
        assertEquals(0, m1.index)
        assertEquals(4, re.lastIndex)

        val m2 = re.exec(string)!!
        assertEquals(listOf("A5D5", "A5", "D5"), m2.asArray().asList())
        assertEquals(string.indexOf(m2[0]!!), m2.index)

        val noMatch = re.exec(string)
        assertEquals(null, noMatch)
        assertEquals(0, re.lastIndex)
    }
}
