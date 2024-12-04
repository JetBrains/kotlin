fun box() = expectThrowableMessage {
    val text: String? = "Hello"
    assert(text?.length?.minus(2) == 1)
}
