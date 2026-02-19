fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (qux() != 42) return "Fail"
        1 -> if (qux() != 99) return "Fail"
        else -> "Unknown"
    }
    return "OK"
}
