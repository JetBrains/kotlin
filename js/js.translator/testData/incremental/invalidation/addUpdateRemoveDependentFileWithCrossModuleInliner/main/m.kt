fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 6 -> if (qux() != 42) return "Fail"
        1, 5 -> if (qux() != 42 + 78 / 2) return "Fail"
        2, 4 -> if (qux() != 42 + 123) return "Fail"
        3, 7 -> if (qux() != 42 + 78) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
