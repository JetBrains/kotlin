fun test(n: Int): String {
    return when {
        n in 0..10 -> "small"
        n in 10..100 -> "average"
        else -> when<caret> {
            n in 100..1000 -> "big"
            n in 1000..10000 -> "very big"
            else -> "unknown"
        }
    }
}