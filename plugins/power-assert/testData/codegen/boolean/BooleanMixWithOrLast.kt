fun box() = expectThrowableMessage {
    val text = "Hello"
    assert((text.length == 5 && text.lowercase() == text) || text.length == 1)
}
