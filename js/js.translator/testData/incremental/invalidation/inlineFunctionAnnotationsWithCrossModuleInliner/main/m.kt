fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 1, 2, 3 -> if (MyClassA().foo() != 42) return "Fail"
        4 -> if (MyClassA().foo() != 33) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
