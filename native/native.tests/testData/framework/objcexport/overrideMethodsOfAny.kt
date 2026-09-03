package overrideMethodsOfAny

import kotlin.test.*

@Throws(Throwable::class)
fun test(obj: Any, other: Any, swift: Boolean) {
    if (!swift) {
        // Doesn't work for Swift, see https://youtrack.jetbrains.com/issue/KT-44613.
        assertEquals(42, obj.hashCode())
        assertTrue(obj.equals(other))
    }

    assertTrue(obj.equals(obj))
    assertFalse(obj.equals(null))
    assertFalse(obj.equals(Any()))

    assertEquals("toString", obj.toString())
}

// The cases above are rooted in NSObject. A subclass of an *exported Kotlin class* is a different
// path: its TypeInfo is synthesized with the reverse adapters of every supertype, including
// `kotlin.Any`'s, so `DescribedByKotlin`'s own adapter and `kotlin.Any`'s compete for the same
// vtable slot -- and -[KotlinBase description] must reach Kotlin rather than the selector it was
// entered through.
open class DescribedByKotlin {
    override fun toString(): String = "kotlin-described"
    override fun hashCode(): Int = 7
}

@Throws(Throwable::class)
fun testDescribedByKotlin(obj: DescribedByKotlin, expectedToString: String, expectedHashCode: Int) {
    assertEquals(expectedToString, obj.toString())
    assertEquals(expectedHashCode, obj.hashCode())
}
