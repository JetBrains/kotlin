fun box() = expectThrowableMessage {
    var i = 3
    assert(i-- == 4)
}
