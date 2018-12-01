fun stop(): Boolean {
    return false
}

fun foo() {
    while (true) {
        if (!stop()) break
    }
}