fun expectThrowableMessage(block: () -> Unit): String {
    try {
        block()
        throw AssertionError("no failure")
    } catch (e: Throwable) {
        return e.message ?: throw AssertionError("no message")
    }
}
