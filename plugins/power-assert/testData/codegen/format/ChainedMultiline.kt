fun box() = expectThrowableMessage {
    assert(
        listOf("Hello", "World")
            .map { it.lowercase() }
            .first { it.startsWith("w") }
            .length == 4
    )
}
