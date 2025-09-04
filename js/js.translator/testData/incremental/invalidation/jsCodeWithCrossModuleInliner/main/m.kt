fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (foo() != 42) return "Fail"
        1 -> if (foo() != 43) return "Fail"
        2 -> if (foo() != "44") return "Fail"
        3 -> if (foo() != "hello") return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
