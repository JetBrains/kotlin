// ISSUE: KT-88891

// MODULE: lib
// FILE: lib.kt

import lombok.Builder
import lombok.Singular

// A class-level `@Builder` builds out of the primary constructor's value parameters, but the
// annotations that shape a builder field sit on the property promoted from one: `@Builder.Default`
// is `@Target(FIELD)`, and `@Singular` may be written with a `@field:` use-site target. Both have
// to be found on a class read back from another module just as they are on one from source.
@Builder
class LibraryEntity(
    val plain: Int,
    @Builder.Default
    val defaulted: String = "default",
    @field:Singular("tag")
    val tags: List<String>,
)

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.assertEquals

fun box(): String {
    val defaults = LibraryEntity.builder().plain(1).build()
    assertEquals(1, defaults.plain)
    assertEquals("default", defaults.defaulted)
    assertEquals(emptyList<String>(), defaults.tags)

    val filled = LibraryEntity.builder().plain(2).defaulted("set").tag("a").tag("b").build()
    assertEquals(2, filled.plain)
    assertEquals("set", filled.defaulted)
    assertEquals(listOf("a", "b"), filled.tags)

    return "OK"
}
