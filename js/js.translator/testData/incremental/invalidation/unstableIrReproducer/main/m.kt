fun box(stepId: Int): String {
    when (stepId) {
        0 -> if (qux() != 0) return "Fail"
        1 -> if (qux() != 1) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
