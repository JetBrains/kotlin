// FUNCTION: kotlin.require

fun box() = expectThrowableMessage {
    val list = listOf("Jane", "John")
    require(
        value = list
            .map { "Doe, $it" }
            .any { it == "Scott, Michael" }
    )
}
