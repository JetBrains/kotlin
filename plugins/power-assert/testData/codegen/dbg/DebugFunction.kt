// FUNCTION: dbg

fun box() = expectThrowableMessage {
    dbg(1 + 2 + 3)
}

fun <T> dbg(value: T): T = value
fun <T> dbg(value: T, msg: String): T = throw RuntimeException(msg)
