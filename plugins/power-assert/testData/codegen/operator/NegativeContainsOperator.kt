fun box() = expectThrowableMessage {
    assert("Hello" !in listOf("Hello", "World"))
}
