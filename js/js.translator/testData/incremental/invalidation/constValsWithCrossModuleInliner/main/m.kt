fun box(stepId: Int, isWasm: Boolean): String {
    val q: Any = qux()
    val b: Any = bar()
    when (stepId) {
        0 -> {
            if (q != "FOO") return "Fail qux"
            if (b != "FOO") return "Fail bar"
        }
        1 -> {
            if (q != "BAR") return "Fail qux"
            if (b != "BAR") return "Fail bar"
        }
        2 -> {
            if (q != 1) return "Fail qux"
            if (b != 1) return "Fail bar"
        }
        3 -> {
            if (q != 3) return "Fail qux"
            if (b != 3) return "Fail bar"
        }
        else -> return "Unknown"
    }
    return "OK"
}
