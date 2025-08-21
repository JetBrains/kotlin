class B {
    companion object {
        fun foo(): Boolean = false
    }
}

fun box() = expectThrowableMessage {
    assert(B.foo())
}