private fun prop() = "3"

internal inline fun foo(lambda: () -> String = ::prop): String {
    return lambda()
}
