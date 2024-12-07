fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (foo() != (42 + 77)) return "Fail"
        1 -> if (foo() != (42 + 88)) return "Fail"
        2 -> if (foo() != (42 + 88 + 11)) return "Fail"
        3 -> if (foo() != (42 + 88 + 22)) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
