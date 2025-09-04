fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        1 -> if (qux2() != 99 + 32) return "Fail"
        2 -> if (qux2() != 99 + 32 + 42) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
