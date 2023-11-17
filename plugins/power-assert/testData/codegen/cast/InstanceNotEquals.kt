fun box() = expectThrowableMessage {
    assert("Hello, world!" !is String)
}
