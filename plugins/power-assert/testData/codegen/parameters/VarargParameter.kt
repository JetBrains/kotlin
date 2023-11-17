fun box() = expectThrowableMessage {
    var i = 0
    assert(listOf("a", "b", "c") == listOf(i++, i++, i++))
}
