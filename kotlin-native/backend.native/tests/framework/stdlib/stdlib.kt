/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("UNUSED")
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

package stdlib

import kotlin.test.*

fun <K, V> isEmpty(map: Map<K, V>) = map.isEmpty()

fun <K, V> getKeysAsSet(map: Map<K, V>) = map.keys
fun <K, V> getKeysAsList(map: Map<K, V>) = map.keys.toList()

fun <K, V> toMutableMap(map: HashMap<K, V>) = map.toMutableMap()

fun <E> getFirstElement(collection: Collection<E>) = collection.first()

class GenericExtensionClass<K, out V, out T : Map<K, V>> (private val holder: T?) {
    fun getFirstKey(): K? = holder?.entries?.first()?.key

    fun getFirstValue() : V? {
        holder?.entries?.forEach { e -> println("KEY: ${e.key}  VALUE: ${e.value}") }
        return holder?.entries?.first()?.value
    }
}

fun <K, V> createPair():
        Pair<LinkedHashMap<K, V>, GenericExtensionClass<K, V, Map<K, V>>> {
    val l = createLinkedMap<K, V>()
    val g = GenericExtensionClass(l)
    return Pair(l, g)
}

fun <K, V> createLinkedMap() = linkedMapOf<K, V>()

fun createTypedMutableMap() = linkedMapOf<Int, String>()

fun addSomeElementsToMap(map: MutableMap<String, Int>) {
    map.put(key = "XYZ", value = 321)
    map.put(key = "TMP", value = 451)
}

fun list(vararg elements: Any?): Any = listOf(*elements)
fun set(vararg elements: Any?): Any = setOf(*elements)
fun map(vararg keysAndValues: Any?): Any = mutableMapOf<Any?, Any?>().apply {
    (0 until keysAndValues.size step 2).forEach {index ->
        this[keysAndValues[index]] = keysAndValues[index + 1]
    }
}

fun emptyMutableList(): Any = mutableListOf<Any?>()
fun emptyMutableSet(): Any = mutableSetOf<Any?>()
fun emptyMutableMap(): Any = mutableMapOf<Any?, Any?>()

data class TripleVals<T>(val first: T, val second: T, val third: T)

data class TripleVars<T>(var first: T, var second: T, var third: T) {
    override fun toString(): String {
        return "[$first, $second, $third]"
    }
}

fun gc() = kotlin.native.runtime.GC.collect()

// Note: this method checks only some of the operations (namely the ones modified recently,
// and thus required additional tests).
// More tests are absolutely needed.
@Throws(Throwable::class)
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun testSet(set: Set<String>) {
    val setAny: Set<Any?> = set

    assertTrue(set.contains("a"))
    assertTrue(set.contains("c"))
    assertFalse(set.contains("h"))
    assertFalse(setAny.contains(1))

    set as kotlin.native.internal.KonanSet<String> // Smart cast to access getElement below.
    assertEquals("a", set.getElement("a"))
    assertNull(set.getElement("aa"))
    assertNull((setAny as kotlin.native.internal.KonanSet<Any?>).getElement(1))
}

// Note: this method checks only some of the operations (namely the ones modified recently,
// and thus required additional tests).
// More tests are absolutely needed.
@Throws(Throwable::class)
fun testMap(map: Map<String, Int>) {
    val mapAny: Map<String, Any?> = map
    val mapKeysAny: Set<Any?> = map.keys
    val mapEntriesAny: Set<Map.Entry<Any?, Any?>> = map.entries

    assertTrue(map.containsKey("a"))
    assertTrue(map.keys.contains("b"))
    assertTrue(map.containsKey("g"))
    assertFalse(map.containsKey("0"))
    assertFalse(mapKeysAny.contains(1))

    assertTrue(map.containsValue(1))
    assertTrue(map.values.contains(2))
    assertTrue(map.containsValue(7))
    assertFalse(map.containsValue(8))
    assertFalse(mapAny.containsValue("8"))

    assertEquals(2, map.get("b"))
    assertEquals(4, map.get("d"))
    assertNull(map.get("h"))

    val referenceMap = (0 until 7).map { ('a' + it).toString() to (it + 1) }.toMap()
    assertEquals(referenceMap.hashCode(), map.hashCode())
    assertEquals(referenceMap, map)
    assertEquals(map, referenceMap)

    assertEquals(28, map.entries.sumBy { it.value })

    assertTrue(map.entries.contains(createMapEntry("e", 5)))
    assertTrue(map.entries.contains(createMapEntry("g", 7)))
    assertFalse(map.entries.contains(createMapEntry("e", 7)))
    assertFalse(map.entries.contains(createMapEntry("e", 10)))
    assertFalse(map.entries.contains(createMapEntry("10", 5)))
    assertFalse(map.entries.contains(createMapEntry("10", 10)))
    assertFalse(mapEntriesAny.contains(createMapEntry(5, "e")))
}

private fun <K, V> createMapEntry(key: K, value: V) = mapOf(key to value).entries.single()
