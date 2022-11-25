private object Obj {
    val x = "9"

    override fun toString() = x
}

internal inline fun foo(lambda: () -> String = { "$Obj" } ): String {
    return lambda()
}
