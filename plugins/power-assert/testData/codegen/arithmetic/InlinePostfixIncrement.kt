fun box() = expectThrowableMessage {
    var i = 1
    assert(i++ == 4)
}
