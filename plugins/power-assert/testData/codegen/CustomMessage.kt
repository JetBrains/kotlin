fun box() = expectThrowableMessage {
    assert(1 == 2) { "Not equal" }
}
