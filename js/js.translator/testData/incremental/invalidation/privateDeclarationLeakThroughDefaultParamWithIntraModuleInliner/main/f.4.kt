private fun prop() = "3"

internal inline fun foo(lambda: () -> String = { "4" }): String {
    return lambda()
}
