fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        5 -> if (qux() != 14) return "Fail"
        6 -> if (qux() != 104) return "Fail"
        else -> return "Unknown"
    }
    if (baz(77) != 79) return "Fail"
    if (baz("test") != 4) return "Fail"
    return "OK"
}
