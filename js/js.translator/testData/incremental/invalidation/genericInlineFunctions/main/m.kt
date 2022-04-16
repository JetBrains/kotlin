
fun isEqual(l: Any?, r: Any?) = if (l == r) true else null

fun box(stepId: Int): String {
    when (stepId) {
        in 0..5 -> isEqual(foo("test"), 123) ?: return "Fail"
        in 8..13 -> isEqual(foo("test"), 123) ?: return "Fail"
        in 16..21 -> isEqual(foo("test"), 123) ?: return "Fail"
        6, 14, 22 -> isEqual(foo("test"), 99) ?: return "Fail"
        7, 15, 23 -> isEqual(foo("test"), "test") ?: return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
