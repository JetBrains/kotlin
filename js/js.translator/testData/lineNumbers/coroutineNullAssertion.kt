suspend fun foo() {
    delay()
    null!!
    delay()
    println("OK")
}

suspend fun delay() {
}

// LINES(JS_IR): 1 1 * 8 8 9 9 1 * 1 * 2 * 3 3 3 * 5 5 6 6
