var log = ""

fun box(x: String?) {
    log +=
            x
            ?:
            try { foo() } finally { log += "finally" }
}

fun foo() = "bar"

// LINES(JS_IR): 1 1 1 1 1 1 1 1 * 3 3 4 * 5 5 * 7 7 7 * 5 4 4 10 10 10 10 * 1
