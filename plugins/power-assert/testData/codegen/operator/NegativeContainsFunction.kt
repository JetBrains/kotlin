fun box() = expectThrowableMessage {
    assert(!listOf("Hello", "World").contains("Hello"))
}
