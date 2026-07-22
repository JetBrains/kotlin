// FULL_JDK

import lombok.Builder
import lombok.Singular

@Builder
class User(
    @Singular val numbers: Map<String, Int>,
    @Singular val statuses: List<String>,
    @Singular val tags: Set<String>,
)

@Builder(setterPrefix = "with")
class Other(
    @Singular("singleSome") val some: List<Int>,
    @Singular val names: Iterable<String>,
)

@Builder
class NullableCollection(
    @Singular val items: List<Int>?,
)

@Builder
class IgnoreNullCollections(
    @Singular(ignoreNullCollections = true) val items: List<Int>,
)

@Builder
class IgnoreNullCollectionsWithNullableCollections(
    @Singular(ignoreNullCollections = true) val items: List<Int>?,
)

@Builder
class NullableElements(
    @Singular val items: List<Int?>,
)

@Builder
class SingularCollections(
    @Singular val items: List<String>,
    @Singular val tags: Set<String>,
    @Singular val pairs: Map<String, Int>,
)

fun box(): String {
    val userBuilder = User.builder()
        .status("wrong")
        .clearStatuses()
        .status("hello")
        .statuses(listOf("world", "!"))
        .number("1", 1)
        .numbers(mapOf("2" to 2, "3" to 3))
        .tag("a")
        .tags(setOf("b", "c"))

    val user = userBuilder.build()

    val other = Other.builder()
        .withSingleSome(1)
        .withSome(listOf(2, 3))
        .withName("John")
        .withNames(setOf("Peter"))
        .build()

    assertEquals(mapOf("1" to 1, "2" to 2, "3" to 3), user.numbers)
    assertEquals(listOf("hello", "world", "!"), user.statuses)
    assertEquals(setOf("a", "b", "c"), user.tags)
    assertEquals(listOf(1, 2, 3), other.some)
    assertEquals(listOf("John", "Peter"), other.names.toList())

    // Lombok always initializes empty collections for `@Singular` annotated properties.
    // So do the same for Kotlin properties even if they have nullable types.
    val nullableCollection = NullableCollection.builder()
        .items(listOf(1))
        .item(2)
        .build()
    assertEquals(listOf(1, 2), nullableCollection.items)

    val nullableCollection2 = NullableCollection.builder()
        .item(2)
        .items(listOf(1))
        .build()
    assertEquals(listOf(2, 1), nullableCollection2.items)

    val ignoreNullCollections = IgnoreNullCollections.builder()
        .items(listOf(3))
        .item(4)
        .items(null)
        .build()
    assertEquals(listOf(3, 4), ignoreNullCollections.items)

    val ignoreNullCollectionsWithNullableCollections = IgnoreNullCollectionsWithNullableCollections.builder()
        .items(listOf(5))
        .item(6)
        .items(null)
        .build()
    assertEquals(listOf(5, 6), ignoreNullCollectionsWithNullableCollections.items)

    val nullableElements = NullableElements.builder()
        .items(listOf(7))
        .item(8)
        .item(null)
        .build()
    assertEquals(listOf(7, 8, null), nullableElements.items)

    // An untouched `@Singular` field must build to a non-null, canonical empty collection.
    val empty = SingularCollections.builder().build()
    assertEquals(emptyList<String>(), empty.items)
    assertEquals(emptySet<String>(), empty.tags)
    assertEquals(emptyMap<String, Int>(), empty.pairs)

    // A single element must build to the canonical singleton collection.
    val single = SingularCollections.builder()
        .item("a")
        .tag("x")
        .pair("k", 1)
        .build()
    assertEquals(listOf("a"), single.items)
    assertEquals(setOf("x"), single.tags)
    assertEquals(mapOf("k" to 1), single.pairs)

    // Two or more elements must build to a genuinely unmodifiable collection.
    val multiBuilder = SingularCollections.builder()
        .item("a").item("b")
        .tag("x").tag("y")
        .pair("k", 1).pair("l", 2)
    val multi = multiBuilder.build()
    assertEquals(listOf("a", "b"), multi.items)
    assertEquals(setOf("x", "y"), multi.tags)
    assertEquals(mapOf("k" to 1, "l" to 2), multi.pairs)

    try {
        @Suppress("UNCHECKED_CAST")
        (multi.items as MutableList<String>).add("c")
        return "FAIL: multi.items is not unmodifiable"
    } catch (e: UnsupportedOperationException) {
        // expected
    }

    // Building again after further mutation must not affect the already-built collection (defensive copy).
    multiBuilder.item("c")
    val multi2 = multiBuilder.build()
    assertEquals(listOf("a", "b"), multi.items)
    assertEquals(listOf("a", "b", "c"), multi2.items)

    // `clear...()` reuses the accumulated collection in place rather than resetting to "not yet created".
    val cleared = SingularCollections.builder()
        .item("a")
        .item("b")
        .clearItems()
        .item("z")
        .build()
    assertEquals(listOf("z"), cleared.items)

    return "OK"
}
