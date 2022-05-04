fun box(stepId: Int): String {
    when (stepId) {
        0 -> {
            if (qux() != "FOO") return "Fail qux"
            if (bar() != "FOO") return "Fail bar"
        }
        1 -> {
            if (qux() != "BAR") return "Fail qux"
            if (bar() != "BAR") return "Fail bar"
        }
        else -> return "Unknown"
    }
    return "OK"
}
