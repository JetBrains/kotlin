inline fun <reified T> foo(x: T, y: Any = 99) : T {
    val tmp = y as? T
    if (tmp != null) {
        return tmp
    }
    return x
}
