import foo.bar.ok

fun box(stepId: Int, isWasm: Boolean): String {
    return when (stepId) {
        0 -> ok
        1 -> ok
        2 -> ok
        3 -> ok
        4 -> ok
        5 -> if (ok != "OK2") "Fail" else "OK"
        else -> "Unknown"
    }
}
