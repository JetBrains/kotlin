class B {
    companion object {
        val b: Boolean = false
    }
}

fun box() = expectThrowableMessage {
    assert(B.b)
}