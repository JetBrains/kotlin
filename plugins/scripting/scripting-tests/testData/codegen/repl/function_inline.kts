// SNIPPET

inline fun <R> runInline(block: () -> R): R {
    return block()
}

val res = runInline { "OK" }

// EXPECTED: res == "OK"
