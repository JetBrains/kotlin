// ISSUE: KT-89084
// WITH_GUAVA
// FULL_JDK
// IGNORE_BACKEND_K2: JVM

// FILE: main.kt
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.ImmutableSortedSet
import lombok.Builder
import lombok.Singular
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// With `lombok.singular.useGuava=true`, a `@Singular` collection is built as a Guava immutable collection.
@Builder
class UseGuavaConfigTarget(
    @Singular("word") val words: List<String>,
    @Singular("element") val elements: Collection<String>,
    @Singular("iterableElement") val iterable: Iterable<String>,
    @Singular("tag") val tags: Set<String>,
    @Singular("mapping") val mappings: Map<String, Int>,
    @Singular("sortedItem") val sorted: java.util.SortedSet<String>,
    @Singular("navigableItem") val navigable: java.util.NavigableSet<String>,
    @Singular("sortedMapping") val sortedMappings: java.util.SortedMap<String, Int>,
)

fun box(): String {
    val target = UseGuavaConfigTarget.builder()
        .word("b").word("a")
        .element("b").element("a")
        .iterableElement("b").iterableElement("a")
        .tag("b").tag("a")
        .mapping("b", 2).mapping("a", 1)
        .sortedItem("b").sortedItem("a")
        .navigableItem("b").navigableItem("a")
        .sortedMapping("b", 2).sortedMapping("a", 1)
        .build()

    assertTrue(target.words is ImmutableList<String>)
    assertTrue(target.elements is ImmutableList<String>)
    assertTrue(target.iterable is ImmutableList<String>)
    assertTrue(target.tags is ImmutableSet<String>)
    assertTrue(target.mappings is ImmutableMap<String, Int>)
    assertTrue(target.sorted is ImmutableSortedSet<String>)
    assertTrue(target.navigable is ImmutableSortedSet<String>)
    assertTrue(target.sortedMappings is ImmutableSortedMap<String, Int>)

    assertEquals(listOf("b", "a"), target.words)
    assertEquals(listOf("a", "b"), target.sorted.toList())
    assertEquals(listOf("a", "b"), target.sortedMappings.keys.toList())

    // As in Lombok's Guava singularizers, the add-all method of a collection takes any `Iterable`.
    val letters = object : Iterable<String> {
        override fun iterator(): Iterator<String> = listOf("x", "y").iterator()
    }
    assertEquals(listOf("x", "y"), UseGuavaConfigTarget.builder().words(<!ARGUMENT_TYPE_MISMATCH!>letters<!>).build().words)

    val empty = UseGuavaConfigTarget.builder().build()
    assertTrue(empty.words is ImmutableList<String>)
    assertTrue(empty.words.isEmpty())
    assertTrue(empty.sorted is ImmutableSortedSet<String>)

    return "OK"
}

// FILE: lombok.config
lombok.singular.useGuava=true
