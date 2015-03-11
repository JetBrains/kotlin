package foo


// CHECK_CONTAINS_NO_CALLS: test1
// CHECK_CONTAINS_NO_CALLS: test2
// CHECK_CONTAINS_NO_CALLS: test3

inline fun concat(vararg strings: String): String {
    var result = ""

    for (string in strings) {
        result += string
    }

    return result
}

fun test1(): String {
    return concat()
}

fun test2(): String {
    return concat("a", "b", "c")
}

fun test3(list: Array<String>): String {
    return concat(*list)
}

fun box(): String {
    assertEquals("", test1())
    assertEquals("abc", test2())
    assertEquals("abcd", test3(array("a", "b", "c", "d")))

    return "OK"
}