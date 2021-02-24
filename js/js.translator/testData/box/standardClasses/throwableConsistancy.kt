// IGNORE_BACKEND: JS



fun test(case: String, expctedMessage: String?, expectedCause: Throwable?, expectedToString: String, t: Throwable): String {
    val actualMessage = t.message
    if (actualMessage != expctedMessage) return "$case FAIL message: $actualMessage, expcted: $expctedMessage"

    val actualCause = t.cause
    if (actualCause != expectedCause) return "$case FAIL cause: $actualCause, expcted: $expectedCause"

    val actualToString = t.toString()
    if (actualToString != expectedToString) return "$case FAIL toString: $actualToString, expcted: $expectedToString"

    return "OK"
}


fun box(): String {


    var result: String = "FAIL"

    result = test("Throwable()", null, null, "Throwable", Throwable())
    if (result != "OK") return result

    result = test("Throwable(\"aaaa\")", "aaaa", null, "Throwable: aaaa", Throwable("aaaa"))
    if (result != "OK") return result

    var cause = Throwable()
    result = test("Throwable(Throwable())", "Throwable", cause, "Throwable: Throwable", Throwable(cause))
    if (result != "OK") return result

    cause = Throwable("ccc")
    result = test("Throwable(\"bbbb\", Throwable(\"ccc\"))", "bbbb", cause, "Throwable: bbbb", Throwable("bbbb", cause))
    if (result != "OK") return result

    result = test("Throwable(message = null)", null, null, "Throwable", Throwable(message = null))
    if (result != "OK") return result

    result = test("Throwable(cause = null)", null, null, "Throwable", Throwable(cause = null))
    if (result != "OK") return result

    result = test("Throwable(null, null)", null, null, "Throwable", Throwable(null, null))
    if (result != "OK") return result

    result = test("Throwable(\"eee\", null)", "eee", null, "Throwable: eee", Throwable("eee", null))
    if (result != "OK") return result

    cause = Throwable("ddd")
    result = test("Throwable(null, Throwable(\"ddd\"))", null, cause, "Throwable", Throwable(null, cause))
    if (result != "OK") return result

    return "OK"
}
