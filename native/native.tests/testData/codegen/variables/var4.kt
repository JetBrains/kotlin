import kotlin.test.*

fun box(): String {
    var x = Error()

    for (i in 0..1) {
        val c = Error()
        if (i == 0) x = c
    }

    // x refcount is 1.

    try {
        try {
            throw x
        } finally {
            x = Error()
        }
    } catch (e: Error) {
        e.use()
    }
    return "OK"
}

private fun Any?.use() {
    var x = this
}