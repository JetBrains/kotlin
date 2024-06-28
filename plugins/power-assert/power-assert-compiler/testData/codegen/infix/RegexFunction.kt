fun box() = expectThrowableMessage {
    assert("Hello, World".matches("[A-Za-z]+".toRegex()))
}
