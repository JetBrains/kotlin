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

fun withThrowableMessage(block: () -> String): String {
    val msg = try {
        block()
    } catch (e: Throwable) {
        e.message ?: "no message"
    }
    return "---\n${msg}\n---\n"
}

fun runAllOutput(vararg tests: Pair<String, () -> String>): String {
    return tests.joinToString("") { (name, func) ->
        val msg = withThrowableMessage { func() }
        "${name}: $msg"
    }
}
