fun isEqual(l: Any?, r: Any?) = if (l == r) true else null

fun box(stepId: Int, isWasm: Boolean): String {
    if (isWasm) {
        when (stepId) {
            6, 7, 14, 22 -> return "OK" // Wasm prohibits unsafe cast to invalid T
            else -> Unit
        }
    }

    when (stepId) {
        in 0..5 -> isEqual(foo_proxy("test"), 123) ?: return "Fail"
        in 8..13 -> isEqual(foo_proxy("test"), 123) ?: return "Fail"
        in 16..21 -> isEqual(foo_proxy("test"), 123) ?: return "Fail"
        6, 14, 22 -> isEqual(foo_proxy("test"), 99) ?: return "Fail"
        7, 15, 23 -> isEqual(foo_proxy("test"), "test") ?: return "Fail"
        else -> return "Unknown"
    }
    return "OK"
}
