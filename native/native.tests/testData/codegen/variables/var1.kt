import kotlin.test.*

private class Integer(val value: Int) {
    operator fun inc() = Integer(value + 1)
}

private fun foo(x: Any, y: Any) {
    x.use()
    y.use()
}

fun box(): String {
    var x = Integer(0)

    for (i in 0..1) {
        val c = Integer(0)
        if (i == 0) x = c
    }

    // x refcount is 1.

    foo(x, ++x)
    return "OK"
}

private fun Any?.use() {
    var x = this
}
