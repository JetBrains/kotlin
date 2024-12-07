fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (qux(4, 2) != -8) return "Fail"
        1 -> if (qux(4, 2) != -4) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
