
fun box(stepId: Int): String {
    when (stepId) {
        0 -> if (foo() != 42) return "Fail"
        1 -> if (foo() != 33) return "Fail"
        2 -> if (foo() != 77) return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
