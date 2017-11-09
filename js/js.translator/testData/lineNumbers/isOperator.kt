var log = ""

fun box(a: Any) {
    if (a is String && foo()) {
        log += "OK"
    }
}

inline fun foo(): Boolean {
    log += "foo"
    return true
}

// LINES: 7 4 4 4 10 10 4 11 4 5 5 12 10 10 11 11 * 1 * 1