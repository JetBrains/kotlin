fun expectThrowableMessage(block: () -> Unit): String {
    try {
        block()
        throw AssertionError("no failure")
    } catch (e: Throwable) {
        return "---${e.message}---\n" ?: throw AssertionError("no message")
    }
}
