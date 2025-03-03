fun expectThrowableMessage(block: () -> Unit): String {
    try {
        block()
        throw AssertionError("no failure")
    } catch (e: Throwable) {
        val msg = e.message ?: throw AssertionError("no message")
        return "---${msg}---\n"
    }
}

fun runAll(vararg tests: Pair<String, () -> Unit>): String {
    return tests.joinToString("") { (name, func) ->
        val msg = expectThrowableMessage { func() }
        "${name}: $msg"
    }
}
