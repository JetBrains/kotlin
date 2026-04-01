private object Obj {
    inline val x get() = "8"
}

private inline fun bar() = Obj.x

internal inline fun foo(lambda: () -> String = ::bar): String {
    return lambda()
}
