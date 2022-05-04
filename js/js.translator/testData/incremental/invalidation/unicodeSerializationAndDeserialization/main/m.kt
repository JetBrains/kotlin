fun box(step: Int): String {
    return when (step) {
        0 -> checkStep(step, qux = "\uD800", quz = "\uD800")
        1 -> checkStep(step, qux = "\uDB6A", quz = "\uD800")
        2 -> checkStep(step, qux = "\uDB6A", quz = "\uDFFF")
        else -> "Unknown Step"
    }
}

fun checkStep(step: Int, qux: String, quz: String): String {
    val a = qux()
    val b = quz()

    if (a != qux || a == "?") {
        return "Fail(qux): Step $step"
    }

    if (b != quz || b == "?") {
        return "Fail(quz): Step $step"
    }

    return "OK"
}
