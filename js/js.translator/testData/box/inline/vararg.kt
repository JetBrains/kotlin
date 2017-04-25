// EXPECTED_REACHABLE_NODES: 494
package foo


// CHECK_CONTAINS_NO_CALLS: test1
// CHECK_CONTAINS_NO_CALLS: test2
// CHECK_CONTAINS_NO_CALLS: test3 except=slice

internal inline fun concat(vararg strings: String): String {
    var result = ""

    for (string in strings) {
        result += string
    }

    return result
}

internal fun test1(): String {
    return concat()
}

internal fun test2(): String {
    return concat("a", "b", "c")
}

internal fun test3(list: Array<String>): String {
    return concat(*list)
}

fun box(): String {
    assertEquals("", test1())
    assertEquals("abc", test2())
    assertEquals("abcd", test3(arrayOf("a", "b", "c", "d")))

    return "OK"
}