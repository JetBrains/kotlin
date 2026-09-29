// ISSUE: KT-89117
// WITH_GUAVA
// FULL_JDK

// FILE: Art.java
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

@Builder
@Data
@AllArgsConstructor
public class Art {
    protected String author;
    @Singular
    protected SortedSet<String> sortedKeyValues;
    @Singular
    protected List<String> tags;
    @Singular
    protected Map<String, Integer> scores;
}

// FILE: test.kt
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSortedSet
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// With `lombok.singular.useGuava=true`, Lombok's add-all methods of a `@Singular` collection take an `Iterable`
// rather than a `Collection`: calling the latter from Kotlin fails with `NoSuchMethodError`.
fun box(): String {
    val art = Art.builder()
        .sortedKeyValues(ImmutableSet.of("b", "a"))
        .tags(ImmutableList.of("x", "y"))
        .score("z", 1)
        .scores(ImmutableMap.of("w", 2))
        .build()

    assertTrue(art.sortedKeyValues is ImmutableSortedSet<String>)
    assertEquals(listOf("a", "b"), art.sortedKeyValues.toList())
    assertTrue(art.tags is ImmutableList<String>)
    assertEquals(listOf("x", "y"), art.tags)
    assertEquals(mapOf("z" to 1, "w" to 2), art.scores)

    // Any `Iterable` is accepted, not only a `Collection`.
    val fromIterable = Art.builder().tags(listOf("p", "q").asIterable()).build()
    assertEquals(listOf("p", "q"), fromIterable.tags)

    return "OK"
}

// FILE: lombok.config
lombok.singular.useGuava=true
