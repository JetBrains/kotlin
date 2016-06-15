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
        val map = mutableMapOf(2 to "x", 3 to null)
        assertEquals("x", map.computeIfAbsent(2) { it.toString() })
        assertEquals("3", map.computeIfAbsent(3) { it.toString() })
        assertEquals(null, map.computeIfAbsent(0) { null })
    }

    @Test fun computeIfPresent() {
        val map = mutableMapOf(2 to "x")
        assertEquals("2x", map.computeIfPresent(2) { k, v -> k.toString() + v })
        assertEquals(null, map.computeIfPresent(3) { k, v -> k.toString() + v })
        // fails due to KT-12144
        // assertEquals(null, map.computeIfPresent(2) { k, v -> null })
        // assertFalse(2 in map)
    }

    @Test fun compute() {
        val map = mutableMapOf(2 to "x")
        assertEquals("2x", map.compute(2) { k, v -> k.toString() + v })
        // fails due to KT-12144
        // assertEquals(null, map.compute(2) { k, v -> null })
        // assertFalse { 2 in map }
        assertEquals("1null", map.compute(1) { k, v -> k.toString() + v })
    }

    @Test fun merge() {
        val map = mutableMapOf(2 to "x")
        // fails due to KT-12144
//        assertEquals("y", map.merge(3, "y") { old, new -> null })
//        assertEquals(null, map.merge(3, "z") { old, new ->
//            assertEquals("y", old)
//            assertEquals("z", new)
//            null
//        })
        // assertFalse(3 in map)

        val map2 = mutableMapOf<Int, Any?>(1 to null)
        // new value must be V!!
        assertFails { map2.merge(1, null) { k, v -> 2 } }
    }
}