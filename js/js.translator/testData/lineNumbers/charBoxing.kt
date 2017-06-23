var log = ""

inline fun String.foo(a: String): Int {
    log += "foo1"
    return asDynamic().indexOf(a)
}

inline fun String.foo(a: Char): Int {
    log += "foo2"
    return indexOf(a.toString())
}

fun bar(a: String, b: Char, x: Int) {
    log += if (x > 0)
        23
    else
        a.foo(b)
}

// LINES: 6 4 4 5 5 11 9 9 10 10 18 14 14 14 14 14 15 17 17 9 9 14 10 17 10 14 14 * 1 * 1