var log = ""

fun box(x: String?) {
    log +=
            x
            ?:
            try { foo() } finally { log += "finally" }
}

fun foo() = "bar"

// LINES: 8 7 7 4 4 4 5 5 7 7 7 7 * 5 4 5 10 10 10 * 1 * 1