fun box() = expectThrowableMessage {
    val text = "Hello"
    assert((text.length == 1 || text.toLowerCase() == text) && text.length == 1)
}
