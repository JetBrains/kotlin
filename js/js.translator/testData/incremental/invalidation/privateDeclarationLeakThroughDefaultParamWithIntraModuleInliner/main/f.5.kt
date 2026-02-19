internal inline fun foo(lambda: () -> String = { "5" }): String {
    return lambda()
}
