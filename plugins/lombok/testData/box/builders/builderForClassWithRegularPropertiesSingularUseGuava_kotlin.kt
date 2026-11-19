// ISSUE: KT-89084, KT-90709
// WITH_GUAVA
// FULL_JDK

// FILE: main.kt
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.ImmutableTable
import lombok.Builder
import lombok.Singular
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// With `lombok.singular.useGuava=true`, a `@Singular` regular property is built as a Guava immutable collection,
// exactly as a `@Singular` constructor parameter is (see `builderSingularUseGuavaConfig_kotlin.kt`).
@Builder(toBuilder = true)
class UseGuavaProperties {
    @Singular("word") var words: List<String> = listOf("initial")
    @Singular("element") var elements: Collection<String> = emptyList()
    @Singular("iterableElement") var iterable: Iterable<String> = emptyList()
    @Singular("tag") var tags: Set<String> = emptySet()
    @Singular("mapping") var mappings: Map<String, Int> = emptyMap()
    @Singular("sortedItem") var sorted: java.util.SortedSet<String> = java.util.TreeSet()
    @Singular("navigableItem") var navigable: java.util.NavigableSet<String> = java.util.TreeSet()
    @Singular("sortedMapping") var sortedMappings: java.util.SortedMap<String, Int> = java.util.TreeMap()
    @Singular("nullableWord") var nullableWords: List<String>? = null
    @Singular("immutable") var immutables: ImmutableList<String> = ImmutableList.of()
    @Singular("cell") var cells: ImmutableTable<String, String, Int> = ImmutableTable.of()
}

fun box(): String {
    val builder = UseGuavaProperties.builder()
        .word("b").word("a")
        .element("b").element("a")
        .iterableElement("b").iterableElement("a")
        .tag("b").tag("a")
        .mapping("b", 2).mapping("a", 1)
        .sortedItem("b").sortedItem("a")
        .navigableItem("b").navigableItem("a")
        .sortedMapping("b", 2).sortedMapping("a", 1)
        .nullableWord("n")
        .immutable("i")
        .cell("r", "c", 1)
    val target = builder.build()

    assertTrue(target.words is ImmutableList<String>)
    assertTrue(target.elements is ImmutableList<String>)
    assertTrue(target.iterable is ImmutableList<String>)
    assertTrue(target.tags is ImmutableSet<String>)
    assertTrue(target.mappings is ImmutableMap<String, Int>)
    assertTrue(target.sorted is ImmutableSortedSet<String>)
    assertTrue(target.navigable is ImmutableSortedSet<String>)
    assertTrue(target.sortedMappings is ImmutableSortedMap<String, Int>)
    assertTrue(target.nullableWords is ImmutableList<String>)
    assertTrue(target.immutables is ImmutableList<String>)

    assertEquals(listOf("b", "a"), target.words)
    assertEquals(listOf("a", "b"), target.sorted.toList())
    assertEquals(listOf("a", "b"), target.navigable.toList())
    assertEquals(listOf("a", "b"), target.sortedMappings.keys.toList())
    assertEquals(listOf("n"), target.nullableWords)
    assertEquals(listOf("i"), target.immutables)
    assertEquals(1, target.cells.get("r", "c"))

    // The built entity doesn't share the builder's collections.
    builder.word("c").tag("c").mapping("c", 3).sortedItem("c").cell("r2", "c2", 2)
    assertEquals(listOf("b", "a"), target.words)
    assertEquals(setOf("b", "a"), target.tags)
    assertEquals(setOf("b", "a"), target.mappings.keys)
    assertEquals(listOf("a", "b"), target.sorted.toList())
    assertEquals(1, target.cells.size())
    assertEquals(listOf("b", "a", "c"), builder.build().words)

    // As in Lombok's Guava singularizers, the add-all method of a collection takes any `Iterable`.
    val letters = object : Iterable<String> {
        override fun iterator(): Iterator<String> = listOf("x", "y").iterator()
    }
    assertEquals(listOf("x", "y"), UseGuavaProperties.builder().words(letters).build().words)

    // An untouched `@Singular` property gets an empty Guava collection, not its initializer.
    val empty = UseGuavaProperties.builder().build()
    assertTrue(empty.words is ImmutableList<String>)
    assertTrue(empty.words.isEmpty())
    assertTrue(empty.sorted is ImmutableSortedSet<String>)
    assertTrue(empty.nullableWords is ImmutableList<String>)
    assertTrue(empty.cells.isEmpty)

    // `toBuilder()` copies the entity's collections instead of aliasing them.
    val copy = target.toBuilder().word("z").sortedItem("0").cell("r3", "c3", 3).build()
    assertEquals(listOf("b", "a", "z"), copy.words)
    assertEquals(listOf("0", "a", "b"), copy.sorted.toList())
    assertEquals(2, copy.cells.size())
    assertTrue(copy.words is ImmutableList<String>)
    assertEquals(listOf("b", "a"), target.words)
    assertEquals(1, target.cells.size())

    return "OK"
}

// FILE: lombok.config
lombok.singular.useGuava=true
