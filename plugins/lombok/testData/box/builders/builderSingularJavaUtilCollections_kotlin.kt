// FULL_JDK
// IGNORE_BACKEND_K2: JVM
// ISSUE: KT-89045

import lombok.Builder
import lombok.Singular
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@Builder
class JavaUtilCollections(
    @Singular("iterableItem") val iterable: java.lang.Iterable<String>,
    @Singular("collectionItem") val collection: java.util.Collection<String>,
    @Singular("listItem") val list: java.util.List<String>,
    @Singular("setItem") val set: java.util.Set<String>,
    @Singular("mapping") val map: java.util.Map<String, Int>,
)

@Builder(toBuilder = true)
class SortedSingularTarget(
    @Singular("sortedItem") val sorted: java.util.SortedSet<String>,
    @Singular("sortedMapping") val sortedMappings: java.util.SortedMap<String, String>,
    @Singular("navigableMapping") val navigable: java.util.NavigableMap<String, String>,
    @Singular("navigableItem") val navigableSet: java.util.NavigableSet<String>,
)

fun box(): String {
    val collections = JavaUtilCollections.builder()
        .iterableItem("b").iterableItem("a")
        .collectionItem("b").collectionItem("a")
        .listItem("b").listItem("a")
        .setItem("b").setItem("a").setItem("b")
        .mapping("b", 2).mapping("a", 1)
        .build()
    assertEquals(listOf("b", "a"), (collections.iterable as Iterable<String>).toList())
    assertEquals(listOf("b", "a"), (collections.collection as Collection<String>).toList())
    assertEquals(listOf("b", "a"), collections.list as List<String>)
    assertEquals(setOf("b", "a"), collections.set as Set<String>)
    assertEquals(mapOf("b" to 2, "a" to 1), collections.map as Map<String, Int>)
    assertFailsWith<UnsupportedOperationException> { (collections.list as MutableList<String>).add("c") }

    val sorted = SortedSingularTarget.builder()
        .sortedItem("b").sortedItem("a")
        .sortedMapping("b", "2").sortedMapping("a", "1")
        .navigableMapping("b", "2").navigableMapping("a", "1")
        .navigableItem("b").navigableItem("a")
        .build()
    assertEquals(listOf("a", "b"), sorted.sorted.toList())
    assertEquals(listOf("a", "b"), sorted.sortedMappings.keys.toList())
    assertEquals(listOf("a", "b"), sorted.navigable.keys.toList())
    assertEquals(listOf("a", "b"), sorted.navigableSet.toList())
    assertEquals("a", sorted.navigableSet.first())
    assertFailsWith<UnsupportedOperationException> { sorted.sorted.add("c") }
    assertFailsWith<UnsupportedOperationException> { sorted.navigable.put("c", "3") }

    val empty = SortedSingularTarget.builder().build()
    assertTrue(empty.sorted.isEmpty())
    assertTrue(empty.navigable.isEmpty())

    val extended = sorted.toBuilder().sortedItem("0").navigableMapping("0", "0").build()
    assertEquals(listOf("0", "a", "b"), extended.sorted.toList())
    assertEquals(listOf("0", "a", "b"), extended.navigable.keys.toList())
    assertEquals(listOf("a", "b"), sorted.sorted.toList())

    return "OK"
}
