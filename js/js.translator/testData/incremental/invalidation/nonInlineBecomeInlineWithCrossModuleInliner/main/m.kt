fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 3 -> if (foo() != 42) return "Fail"
        1 -> if (foo() != 33) return "Fail"
        2 -> if (foo() != 77) return "Fail"
        4 -> if (foo() != 100) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
