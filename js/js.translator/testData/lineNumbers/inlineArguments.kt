var log = ""

fun box() {
    foo(bar())
}

inline fun foo(x: Int) {
    log += x
    log += x
}

fun bar() = 23

// LINES: 1 1 1 1 1 1 1 1 * 3 3 * 4 8 8 9 9 7 7 8 8 9 9 12 12 12 12 * 1
