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

package test.properties.delegation.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValByMapExtensionsTest {
    val map: Map<String, String> = hashMapOf("a" to "all", "b" to "bar", "c" to "code")
    val genericMap = mapOf<String, Any?>("i" to 1, "x" to 1.0)

    val a by map
    val b: String by map
    val c: Any by map
    val d: String? by map
    val e: String by map.withDefault { "default" }
    val f: String? by map.withDefault<String, String?> { null }
    // val n: Int by map // prohibited by type system
    val i: Int by genericMap
    val x: Double by genericMap


    @Test fun doTest() {
        assertEquals("all", a)
        assertEquals("bar", b)
        assertEquals("code", c)
        assertEquals("default", e)
        assertEquals(null, f)
        assertEquals(1, i)
        assertEquals(1.0, x)
        assertFailsWith<NoSuchElementException> { d }
    }
}


class VarByMapExtensionsTest {
    val map = hashMapOf<String, Any?>("a" to "all", "b" to null, "c" to 1, "xProperty" to 1.0)
    val map2: MutableMap<String, CharSequence> = hashMapOf("a2" to "all")

    var a: String by map
//    var b: Any? by map
    var c: Int by map
    var d: String? by map
    var a2: String by map2.withDefault { "empty" }
    //var x: Int by map2  // prohibited by type system

    @Test fun doTest() {
        assertEquals("all", a)
//        assertEquals(null, b)
        assertEquals(1, c)
        c = 2
        assertEquals(2, c)
        assertEquals(2, map["c"])

        assertEquals("all", a2)
        map2.remove("a2")
        assertEquals("empty", a2)

        assertFailsWith<NoSuchElementException> { d }
        map["d"] = null
        assertEquals(null, d)
    }
}