fun box() = expectThrowableMessage {
    val greeting: Any = "hello"
    assert(greeting is String && greeting.length == 2)
}
