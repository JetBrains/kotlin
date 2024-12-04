fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 3 -> if (qux1() != 42) return "Fail"
        4 -> if (qux1() != 42 + 99 * 8) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
