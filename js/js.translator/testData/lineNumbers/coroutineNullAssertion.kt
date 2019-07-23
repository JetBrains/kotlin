suspend fun foo() {
    delay()
    null!!
    delay()
    println("OK")
}

suspend fun delay() {
}

// LINES: 1 6 1 1 * 1 6 2 2 2 2 2 * 3 3 4 4 4 4 4 5 5 * 1 6 1 1 1 1 8 9