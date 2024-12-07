fun box() = expectThrowableMessage {
    val text = "Hello"
    assert((text.length == 1 || text.lowercase() == text) && text.length == 1)
}
