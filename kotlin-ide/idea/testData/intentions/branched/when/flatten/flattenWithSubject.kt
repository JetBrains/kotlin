fun test(n: Int): String {
    return when(n) {
        in 0..10 -> "small"
        in 10..100 -> "average"
        <caret>else -> when(n) {
            in 100..1000 -> "big"
            in 1000..10000 -> "very big"
            else -> "unknown"
        }
    }
}