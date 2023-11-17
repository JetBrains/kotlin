// FUNCTION: kotlin.check

fun box() = expectThrowableMessage {
    check(1 == 2) { "the world is broken" }
}
