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

class ReplaceTest {

    @Test fun testSimpleReplace() {
        val target: String
        val pattern: String
        val repl: String

        target = "foobarfobarfoofo1"
        pattern = "fo[^o]"
        repl = "xxx"

        val regex = Regex(pattern)

        assertEquals("foobarxxxarfoofo1", regex.replaceFirst(target, repl))
        assertEquals("foobarxxxarfooxxx", regex.replace(target, repl))
    }

    @Test fun testCaptureReplace() {
        var target: String
        var pattern: String
        var repl: String
        var s: String
        var regex: Regex

        target = "[31]foo;bar[42];[99]xyz"
        pattern = "\\[([0-9]+)\\]([a-z]+)"
        repl = "$2[$1]"

        regex = Regex(pattern)
        s = regex.replaceFirst(target, repl)
        assertEquals("foo[31];bar[42];[99]xyz", s)
        s = regex.replace(target, repl)
        assertEquals("foo[31];bar[42];xyz[99]", s)

        target = "[31]foo(42)bar{63}zoo;[12]abc(34)def{56}ghi;{99}xyz[88]xyz(77)xyz;"
        pattern = "\\[([0-9]+)\\]([a-z]+)\\(([0-9]+)\\)([a-z]+)\\{([0-9]+)\\}([a-z]+)"
        repl = "[$5]$6($3)$4{$1}$2"
        regex = Regex(pattern)
        s = regex.replaceFirst(target, repl)
        assertEquals("[63]zoo(42)bar{31}foo;[12]abc(34)def{56}ghi;{99}xyz[88]xyz(77)xyz;", s)
        s = regex.replace(target, repl)
        assertEquals("[63]zoo(42)bar{31}foo;[56]ghi(34)def{12}abc;{99}xyz[88]xyz(77)xyz;", s)
    }

    @Test fun testEscapeReplace() {
        val target: String
        val pattern: String
        var repl: String
        var s: String

        target = "foo'bar''foo"
        pattern = "'"
        repl = "\\'"
        s = target.replace(pattern.toRegex(), repl)
        assertEquals("foo'bar''foo", s)
        repl = "\\\\'"
        s = target.replace(pattern.toRegex(), repl)
        assertEquals("foo\\'bar\\'\\'foo", s)
        repl = "\\$3"
        s = target.replace(pattern.toRegex(), repl)
        assertEquals("foo$3bar$3$3foo", s)
    }
}
