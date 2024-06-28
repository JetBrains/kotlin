fun box() = expectThrowableMessage {
    val text = "Hello"
    assert((text.length == 5 && text.toLowerCase() == text) || text.length == 1)
}
