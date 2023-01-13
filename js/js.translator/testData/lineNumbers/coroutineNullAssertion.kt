suspend fun foo() {
    delay()
    null!!
    delay()
    println("OK")
}

suspend fun delay() {
}

// LINES(JS):    1 6 1 1 * 1 6 2 2 2 2 2 * 3 3 4 4 4 4 4 5 5 * 1 6 1 1 1 1 8 9
// LINES(JS_IR): 1 1 * 8 8 9 9 1 * 1 * 2 * 3 3 3 * 5 5 6 6
