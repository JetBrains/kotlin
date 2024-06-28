fun box() = expectThrowableMessage {
    val text: String? = null
    assert(text != null && text.length == 1)
}
