fun test(n: Int): String {
    return <caret>when {
        n !is Int -> "???"
        n in 0..10 -> "small"
        n in 10..100 -> "average"
        n in 100..1000 -> "big"
        n == 1000000 -> "million"
        n !in -100..-10 -> "good"
        n is Int -> "unknown"
        else -> "unknown"
    }
}