// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1519
package foo



fun <T> test(a: List<T>, b: List<T>, removed: Boolean, expected: List<T>): String? {
    val t = ArrayList<T>(a.size)
    t.addAll(a)

    if (t.removeAll(b) != removed) return "$a.removeAll($b) != $removed, result list: $t"
    if (t != expected) return "Wrong result of $a.removeAll($b), expected: $expected, actual: $t"

    return null
}

fun box(): String {
    val list = listOf(3, "2", -1, null, 0, 8, 5, "3", 77, -15)
    val subset = listOf(3, "2", -1, null)
    val empty = listOf<Any?>()
    val withOtherElements = listOf(3, 54, null)

    return test(list, subset, removed = true, expected = listOf(0, 8, 5, "3", 77, -15)) ?:
    test(list, empty, removed = false, expected = list) ?:
    test(list, withOtherElements, removed = true, expected = listOf("2", -1, 0, 8, 5, "3", 77, -15)) ?:
    "OK"
}
