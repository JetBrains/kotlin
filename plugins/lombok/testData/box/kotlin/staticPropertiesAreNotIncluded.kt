// LANGUAGE: +CompanionBlocks +CompanionExtensions
// ISSUE: KT-88367

// A `companion { }` block declares statics on the class itself rather than members of a companion object, so
// every generator that walks the class's properties sees them. Lombok never includes a static field in the
// members it generates, and including one is not merely cosmetic: `@ToString`/`@EqualsAndHashCode` used to
// crash outright, because IR calls a property's getter with a dispatch receiver that a static getter has no
// slot for.

import lombok.EqualsAndHashCode
import lombok.ToString
import kotlin.test.assertEquals

@ToString
class ToStringWithStatic(val instance: Int) {
    companion {
        val static = 1
    }
}

@EqualsAndHashCode
class EqualsAndHashCodeWithStatic(val instance: Int) {
    companion {
        var static = 2
    }
}

fun box(): String {
    assertEquals("ToStringWithStatic(instance=42)", ToStringWithStatic(42).toString())

    EqualsAndHashCodeWithStatic.static = 10
    val hashCode1 = EqualsAndHashCodeWithStatic(5).hashCode()

    EqualsAndHashCodeWithStatic.static = 11
    val hashCode2 = EqualsAndHashCodeWithStatic(5).hashCode()

    assertEquals(hashCode1, hashCode2) // static fields should not be considered

    // The statics themselves stay reachable, they are simply not part of what Lombok generates.
    assertEquals(1, ToStringWithStatic.static)

    return "OK"
}
