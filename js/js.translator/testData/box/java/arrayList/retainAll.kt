// EXPECTED_REACHABLE_NODES: 889
package foo


fun <T> test(a: List<T>, b: List<T>, removed: Boolean, expected: List<T>): String? {
    val t = ArrayList<T>(a.size)
    t.addAll(a)

    if (t.retainAll(b) != removed) return "$a.retainAll($b) != $removed, result list: $t"
    if (t != expected) return "Wrong result of $a.retainAll($b), expected: $expected, actual: $t"

    return null
}

fun box(): String {
    val list = listOf(3, "2", -1, null, 0, 8, 5, "3", 77, -15)
    val subset = listOf(3, "2", -1, null)
    val empty = listOf<Any?>()
    val withOtherElements = listOf(3, 54, null)

    return test(list, subset, removed = true, expected = subset) ?:
    test(list, empty, removed = true, expected = empty) ?:
    test(list, withOtherElements, removed = true, expected = listOf(3, null)) ?:
    test(list, list, removed = false, expected = list) ?:
    "OK"
}
