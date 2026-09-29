// WITH_GUAVA
// FULL_JDK
// IGNORE_BACKEND_K2: JVM

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import lombok.Builder
import lombok.Singular
import kotlin.test.assertEquals

// A `@Singular` field declared with a Guava type gets an add-all method taking any `Iterable`, as in Lombok, so one
// that is not a `Collection` has to be accepted as well.
@Builder
class GuavaDeclared(
    @Singular("item") val items: ImmutableList<String>,
    @Singular("tag") val tags: ImmutableSet<String>,
)

class Letters(private vararg val letters: String) : Iterable<String> {
    override fun iterator(): Iterator<String> = letters.iterator()
}

fun box(): String {
    val built = GuavaDeclared.builder()
        .item("a").items(Letters("b", "c"))
        .tags(Letters("x")).tags(listOf("y"))
        .build()
    assertEquals(listOf("a", "b", "c"), built.items)
    assertEquals(setOf("x", "y"), built.tags)
    return "OK"
}
