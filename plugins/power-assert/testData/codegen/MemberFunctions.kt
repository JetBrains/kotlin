fun box() = expectThrowableMessage {
    val hello = "Hello"
    assert(hello.length == "World".substring(1, 4).length)
}
