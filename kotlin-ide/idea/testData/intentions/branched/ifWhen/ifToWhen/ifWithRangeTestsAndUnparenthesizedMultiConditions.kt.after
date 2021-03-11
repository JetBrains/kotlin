fun test(n: Int): String {
    return <caret>when (n) {
        in 0..5, in 5..10 -> "small"
        in 10..50, in 50..100 -> "average"
        in 100..500, in 500..1000 -> "big"
        else -> "unknown"
    }
}