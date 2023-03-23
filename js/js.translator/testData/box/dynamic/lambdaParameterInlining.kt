// WITH_STDLIB

fun demo(x: dynamic, a: Array<out dynamic>): Boolean? {
    return a.any { y: Any ->
        val newX: Any = x.unsafeCast<Any>()
        y == newX
    }
}

data class X(val x: Int)

fun box(): String {

    if (demo(X(1), arrayOf(X(1))) != true) return "fail"

    return "OK"
}