fun box() = expectThrowableMessage {
    val hello = listOf("Hello", "World")
    assert(hello.reversed() == emptyList<String>())
}
