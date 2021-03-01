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
