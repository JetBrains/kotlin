var log = ""

fun box() {
    foo(bar())
}

inline fun foo(x: Int) {
    log += x
    log += x
}

fun bar() = 23

// LINES: 5 4 4 8 8 9 9 10 8 8 9 9 12 12 12 * 1 * 1