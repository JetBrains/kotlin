// ISSUE: KT-88270
// WITH_GUAVA
// FULL_JDK

import com.google.common.collect.HashBasedTable
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Table
import lombok.Builder
import lombok.Singular

@Builder(toBuilder = true)
class GuavaCollections(
    @Singular val items: ImmutableList<String>,
    @Singular val values: ImmutableCollection<String>,
    @Singular val tags: ImmutableSet<String>,
    @Singular val ranks: ImmutableSortedSet<Int>,
    @Singular val numbers: ImmutableMap<String, Int>,
    @Singular val codes: ImmutableBiMap<String, Int>,
    @Singular val scores: ImmutableSortedMap<String, Int>,
    @Singular val cells: ImmutableTable<String, String, String>,
)

@Builder
class NullableGuavaCollection(
    @Singular val items: ImmutableList<Int>?,
)

fun box(): String {
    val empty = GuavaCollections.builder().build()
    assertEquals(ImmutableList.of<String>(), empty.items)
    assertEquals(ImmutableList.of<String>(), empty.values)
    assertEquals(ImmutableSet.of<String>(), empty.tags)
    assertEquals(ImmutableSortedSet.of<Int>(), empty.ranks)
    assertEquals(ImmutableMap.of<String, Int>(), empty.numbers)
    assertEquals(ImmutableBiMap.of<String, Int>(), empty.codes)
    assertEquals(ImmutableSortedMap.of<String, Int>(), empty.scores)
    assertEquals(ImmutableTable.of<String, String, String>(), empty.cells)

    val builder = GuavaCollections.builder()
        .item("a").item("b")
        .value("x").value("y")
        .tag("t1").tag("t2")
        .rank(3).rank(1).rank(2)
        .number("one", 1).numbers(mapOf("two" to 2))
        .code("A", 1).codes(mapOf("B" to 2))
        .score("k2", 2).score("k1", 1)
        .cell("r1", "c1", "v1").cells(ImmutableTable.of("r2", "c2", "v2"))

    val result = builder.build()

    assertEquals(listOf("a", "b"), result.items.toList())
    assertEquals(listOf("x", "y"), result.values.toList())
    assertEquals(setOf("t1", "t2"), result.tags)
    assertEquals(listOf(1, 2, 3), result.ranks.toList())
    assertEquals(mapOf("one" to 1, "two" to 2), result.numbers)
    assertEquals(mapOf("A" to 1, "B" to 2), result.codes)
    assertEquals(listOf("k1", "k2"), result.scores.keys.toList())
    val expectedCells = HashBasedTable.create<String, String, String>().apply {
        put("r1", "c1", "v1")
        put("r2", "c2", "v2")
    }
    assertEquals(expectedCells, result.cells)

    try {
        @Suppress("UNCHECKED_CAST")
        (result.items as MutableList<String>).add("c")
        return "FAIL: items is not immutable"
    } catch (e: UnsupportedOperationException) {
        // expected
    }

    try {
        @Suppress("UNCHECKED_CAST")
        (result.numbers as MutableMap<String, Int>)["three"] = 3
        return "FAIL: numbers is not immutable"
    } catch (e: UnsupportedOperationException) {
        // expected
    }

    try {
        @Suppress("UNCHECKED_CAST")
        (result.cells as Table<String, String, String>).put("x", "y", "z")
        return "FAIL: cells is not immutable"
    } catch (e: UnsupportedOperationException) {
        // expected
    }

    builder.item("c")
    builder.cell("r3", "c3", "v3")
    val result2 = builder.build()
    assertEquals(listOf("a", "b"), result.items.toList())
    assertEquals(listOf("a", "b", "c"), result2.items.toList())
    assertEquals(expectedCells, result.cells)
    val expectedCells2 = HashBasedTable.create<String, String, String>().apply {
        putAll(expectedCells)
        put("r3", "c3", "v3")
    }
    assertEquals(expectedCells2, result2.cells)

    val cleared = GuavaCollections.builder()
        .item("a").item("b")
        .clearItems()
        .item("z")
        .value("x").tag("t").rank(1).number("k", 1).code("k", 1).score("k", 1)
        .cell("r", "c", "v").clearCells().cell("rz", "cz", "vz")
        .build()
    assertEquals(listOf("z"), cleared.items.toList())
    val expectedClearedCells = HashBasedTable.create<String, String, String>().apply { put("rz", "cz", "vz") }
    assertEquals(expectedClearedCells, cleared.cells)

    val rebuilder = result.toBuilder()
    rebuilder.item("extra")
    rebuilder.cell("r4", "c4", "v4")
    val rebuilt = rebuilder.build()
    assertEquals(listOf("a", "b"), result.items.toList())
    assertEquals(listOf("a", "b", "extra"), rebuilt.items.toList())
    assertEquals(expectedCells, result.cells)
    val expectedRebuiltCells = HashBasedTable.create<String, String, String>().apply {
        putAll(expectedCells)
        put("r4", "c4", "v4")
    }
    assertEquals(expectedRebuiltCells, rebuilt.cells)

    val nullableEmpty = NullableGuavaCollection.builder().build()
    assertEquals(ImmutableList.of<Int>(), nullableEmpty.items)
    val nullableFilled = NullableGuavaCollection.builder().item(1).item(2).build()
    assertEquals(listOf(1, 2), nullableFilled.items!!.toList())

    return "OK"
}
