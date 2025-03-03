enum class Status(val isActive: Boolean) {
    ACTIVE(true), INACTIVE(false)
}

fun box() = expectThrowableMessage {
    assert(Status.INACTIVE.isActive)
}