fun box() = expectThrowableMessage {
    val list = listOf("Jane", "John")
    assert(list.map { "Doe, $it" }.any { it == "Scott, Michael" })
}
