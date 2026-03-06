// FUNCTION: kotlin.require

fun box() = expectThrowableMessage {
    require(1 == 2) { "the world is broken" }
}
