fun box() = expectThrowableMessage {
    val text: String? = "Hello"
    assert(text == null || text.length == 1 || text.toLowerCase() == text)
}
