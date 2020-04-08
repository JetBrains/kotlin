fun test(n: Int): String {
    return <caret>when {
        n == 0 -> "zero"
        1 == n -> "one"
        n == 2 -> "two"
        else -> "unknown"
    }
}