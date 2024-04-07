import kotlin.test.*

fun box(): String {
    // Try to trick devirtualizer
    assertEquals("[1]", anyMethods(arrayListOf("1")))
    assertEquals("[2]", anyMethods(mapOf("2" to 1).keys))
    assertEquals("[3]", anyMethods(mapOf("1" to 3).values))
    assertEquals("[4]", anyMethods(setOf("4")))
    return "OK"
}

private fun anyMethods(iterable: Iterable<*>): String {
    assertTrue(iterable.equals(iterable))
    assertTrue(iterable.hashCode() != 0)
    return iterable.toString()
}
