// WITH_REFLECT

fun box() = expectThrowableMessage {
    assert(Boolean::class.isOpen)
}
