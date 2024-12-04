fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 1, 2, 3, 4 -> if (foo() != stepId) return "Fail"
        5 -> if (foo() != 4) return "Fail"
        else -> return "Unkown"
    }
    return "OK"
}
