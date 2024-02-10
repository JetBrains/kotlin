fun box() = expectThrowableMessage {
    assert(object { override fun toString() = "ANONYMOUS" }.toString() == "toString()")
}
