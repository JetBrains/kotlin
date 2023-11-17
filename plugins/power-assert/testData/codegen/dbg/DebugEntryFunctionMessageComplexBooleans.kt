// FUNCTION: dbg

fun box() = expectThrowableMessage {
    val greeting: String? = null
    val name: String? = null
    dbg(
        key = greeting != null && greeting.length == 5,
        value = name == null || name.length == 5,
        msg = "Message:"
    )
}

fun <T> dbg(key: Any, value: T): T = value
fun <T> dbg(key: Any, value: T, msg: String): T = throw RuntimeException(key.toString() + "=" + value + "\n" + msg)
