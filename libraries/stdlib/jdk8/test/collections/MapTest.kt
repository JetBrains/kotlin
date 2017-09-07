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

package kotlin.jdk8.collections.test

import org.junit.Test
import java.util.function.BiFunction
import kotlin.test.*
import kotlin.jdk8.collections.*

class MapTest {

    @Test fun getOrDefault() {
        val map = mapOf("x" to 1, "z" to null)
        assertEquals(1, map.getOrDefault("x", 0))
        assertEquals(0, map.getOrDefault("y", 0))
        assertEquals(null, map.getOrDefault("z", 0))
        assertEquals(null, map.getOrDefault("y" as CharSequence, null))

        val nonNullMap = mapOf("x" to 1)
        assertEquals(null, nonNullMap.getOrDefault("y" as CharSequence, null))  // should pass
        assertEquals("none", nonNullMap.getOrDefault("y" as CharSequence, "none"))  // should not compile
    }

    @Test fun forEach() {
        val map = mapOf("k" to "v")
        map.forEach { k, v ->
            assertEquals("k", k)
            assertEquals("v", v)
        }
        map.forEach {
            assertEquals("k", it.key)
            assertEquals("v", it.value)
        }
    }

    @Test fun replaceAll() {
        val map: MutableMap<String, CharSequence> = mutableMapOf("a" to "b", "c" to "d")

        map.replaceAll { k, v -> k + v }
        assertEquals(mapOf<String, CharSequence>("a" to "ab", "c" to "cd"), map)

        val operator = BiFunction<Any, Any, String> { k, v -> k.toString() + v.toString() }
        map.replaceAll(operator)
        assertEquals(mapOf<String, CharSequence>("a" to "aab", "c" to "ccd"), map)
    }

    @Test fun putIfAbsent() {
        val map = mutableMapOf(1 to "a")

        assertEquals("a", map.putIfAbsent(1, "b"))
        assertEquals("a", map[1])

        assertEquals(null, map.putIfAbsent(2, "b"))
        assertEquals("b", map[2])
    }


    @Test fun removeKeyValue() {
        val map = mutableMapOf(1 to "a")

        assertEquals(false, map.remove(1 as Number, null as Any?)) // requires import
        assertEquals(true,  map.remove(1, "a"))
    }

    @Test fun replace() {
        val map = mutableMapOf(1 to "a", 2 to null)
        assertTrue(map.replace(2, null, "x"))
        assertEquals("x", map[2])

        assertFalse(map.replace(2, null, "x"))

        assertEquals("a", map.replace(1, "b"))
        assertEquals(null, map.replace(3, "c"))

    }

    @Test fun computeIfAbsent() {
        val map = mutableMapOf(2 to "x")
        assertEquals("x", map.computeIfAbsent(2) { it.toString() })
        assertEquals("3", map.computeIfAbsent(3) { it.toString() })
        // prohibited: map.computeIfAbsent(0) { null }

        val map2 = mutableMapOf(2 to "x", 3 to null)
        assertEquals("x", map2.computeIfAbsent(2) { it.toString() })
        assertEquals("3", map2.computeIfAbsent(3) { it.toString() })
        assertEquals(null, map2.computeIfAbsent(0) { null })
        assertFalse(0 in map2)
    }

    @Test fun computeIfPresent() {
        val map = mutableMapOf(2 to "x")
        assertEquals("2x", map.computeIfPresent(2) { k, v -> k.toString() + v })
        assertEquals(null, map.computeIfPresent(3) { k, v -> k.toString() + v })
        assertEquals(null, map.computeIfPresent(2) { _, _ -> null })
        assertFalse(2 in map)

        val map2 = mutableMapOf<Int, String?>(2 to "x")
        assertEquals("2x", map2.computeIfPresent(2) { k, v -> k.toString() + v })
        assertEquals(null, map2.computeIfPresent(3) { k, v -> k.toString() + v })
        assertEquals(null, map2.computeIfPresent(2) { _, _ -> null })
        assertFalse(2 in map2)
    }

    @Test fun compute() {
        val map = mutableMapOf(2 to "x")
        assertEquals("2x", map.compute(2) { k, v -> k.toString() + v })
        assertEquals(null, map.compute(2) { _, _ -> null })
        assertFalse { 2 in map }
        assertEquals("1null", map.compute(1) { k, v -> k.toString() + v })
    }

    @Test fun merge() {
        val map = mutableMapOf(2 to "x")
        assertEquals("y", map.merge(3, "y") { _, _ -> null })
        assertEquals(null, map.merge(3, "z") { old, new ->
            assertEquals("y", old)
            assertEquals("z", new)
            null
        })
        assertFalse(3 in map)

        // fails due to KT-12144
        val map2 = mutableMapOf<Int, String?>(1 to null)
        // new value must be V&Any
        assertEquals("e", map2.merge(1, "e") { old, new -> (old.length + new.length).toString() ?: null })
        assertEquals("3", map2.merge(1, "fg") { old, new -> (old.length + new.length).toString() ?: null })
        assertEquals(null, map2.merge(1, "3") { _, _ -> null })
        assertFalse(1 in map)
    }
}
