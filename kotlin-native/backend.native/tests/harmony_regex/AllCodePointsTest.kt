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

class AllCodePointsTest {

    fun assertTrue(msg: String, value: Boolean) = assertTrue(value, msg)
    fun assertFalse(msg: String, value: Boolean) = assertFalse(value, msg)

    fun codePointToString(codePoint: Int): String {
        val charArray = Char.toChars(codePoint)
        return charArray.concatToString(0, charArray.size)
    }

    // TODO: Here is a performance problem: an execution of this test requires much more time than it in Kotlin/JVM.
    @Test fun test() {
        // Regression for HARMONY-3145
        var p = Regex("(\\p{all})+")
        var res = true
        var cnt = 0
        var s: String
        for (i in 0..1114111) {
            s = codePointToString(i)
            if (!s.matches(p)) {
                cnt++
                res = false
            }
        }
        assertTrue(res)
        assertEquals(0, cnt)

        p = Regex("(\\P{all})+")
        res = true
        cnt = 0

        for (i in 0..1114111) {
            s = codePointToString(i)
            if (!s.matches(p)) {
                cnt++
                res = false
            }
        }

        assertFalse(res)
        assertEquals(0x110000, cnt)
    }
}