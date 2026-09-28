// DUMP_KT_IR

import lombok.Builder
import lombok.Singular
import kotlin.test.assertEquals

// A builder field of a Kotlin class is a primary constructor value parameter, `val` or not: `build()` has that
// constructor to call and nothing else. A property declared in the body is computed from the parameters that
// were passed and gets no builder field of its own.
@Builder
class Mixed(val a: Int, b: String, var c: Long) {
    val fromParameter: String = b + "!"
    val computed: Int = a * 2

    val b: String get() = fromParameter.dropLast(1)
}

@Builder
class SingularWithoutVal(id: Int, @Singular("tag") tags: List<String>) {
    val description: String = "$id: ${tags.joinToString()}"
}

// `toBuilder()` copies each builder field off the entity's property of that name, so a parameter without
// `val` round-trips as long as the class declares a property for it by hand. Without one there is nothing to
// read and `TO_BUILDER_CANNOT_OBTAIN` rejects the class - see `builderChecks_kotlin.kt`.
@Builder(toBuilder = true)
class RoundTrip(val kept: Int, mirrored: Int) {
    val mirrored: Int = mirrored
}

fun box(): String {
    val mixed = Mixed.builder().a(2).b("x").c(7L).build()
    assertEquals(2, mixed.a)
    assertEquals(7L, mixed.c)
    assertEquals("x!", mixed.fromParameter)
    assertEquals(4, mixed.computed)
    assertEquals("x", mixed.b)

    // A body property is no builder field: only the three parameters have setters.
    assertEquals(0, Mixed.builder().b("y").build().a)

    val singular = SingularWithoutVal.builder().id(3).tag("a").tag("b").build()
    assertEquals("3: a, b", singular.description)

    val original = RoundTrip(1, 5)
    val rebuilt = original.toBuilder().build()
    assertEquals(1, rebuilt.kept)
    assertEquals(5, rebuilt.mirrored)

    return "OK"
}
