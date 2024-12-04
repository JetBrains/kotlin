fun box(stepId: Int, isWasm: Boolean): String {
    when(stepId) {
        0, 1 -> if (foo() != 42) return "Fail"
        2 -> if (foo() != 77) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
