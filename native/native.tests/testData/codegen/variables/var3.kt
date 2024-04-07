import kotlin.test.*

fun box(): String {
    foo().use()
    return "OK"
}

private fun foo(): Any {
    var x = Any()

    for (i in 0..1) {
        val c = Any()
        if (i == 0) x = c
    }

    // x refcount is 1.

    try {
        return x
    } finally {
        x = Any()
    }
}

private fun Any?.use() {
    var x = this
}