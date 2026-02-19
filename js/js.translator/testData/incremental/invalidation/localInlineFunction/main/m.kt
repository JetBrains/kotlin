fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0 -> if (foo() != "OK-0") return "Fail"
        1 -> if (foo() != "OK-1") return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
