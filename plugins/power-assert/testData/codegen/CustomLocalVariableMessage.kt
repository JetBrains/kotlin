fun box() = expectThrowableMessage {
    val lambda = { "Not equal" }
    assert(1 == 2, lambda)
}
