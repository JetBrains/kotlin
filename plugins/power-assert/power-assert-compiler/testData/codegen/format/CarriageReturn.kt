// !!! File must contain carriage returns as line endings !!!

fun box() = expectThrowableMessage {
    assert(
        listOf("Hello", "World")
            .map { it.lowercase() }
            .first { it.startsWith("w") }
            .length == 4
    )
}
