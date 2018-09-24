suspend fun foo() {
    delay()
    null!!
    delay()
    println("OK")
}

suspend fun delay() {
}

// LINES: 6 1 1 1 1 6 1 1 * 6 2 2 2 2 2 * 3 3 4 4 4 2 4 4 5 5 * 9