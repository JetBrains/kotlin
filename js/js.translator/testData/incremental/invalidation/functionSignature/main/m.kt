fun isEqual(l: Any?, r: Any?) = if (l == r) true else null

fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 8, 15, 16 -> isEqual(foo(), 42) ?: return "Fail"
        1 -> isEqual(foo(), false) ?: return "Fail"
        2 -> isEqual(foo(), 3.14) ?: return "Fail"
        3, 5, 6, 12 -> isEqual(foo(), "string") ?: return "Fail"
        4 -> isEqual(foo(), null) ?: return "Fail"
        7 -> {
            foo() // Unit
            return "OK"
        }
        9 -> isEqual(foo(), 42 + 77) ?: return "Fail"
        10 -> isEqual(foo(), 42 + 98 + 1) ?: return "Fail"
        11 -> isEqual(foo(), 42 + 91) ?: return "Fail"
        13 -> isEqual(foo(), 99) ?: return "Fail"
        14 -> isEqual(foo(), 1L) ?: return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
