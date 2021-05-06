val bar: Any
    get() = Unresolved(id(42))

fun <T> id(x: T): T = x
