private inline fun prop() = "7"

internal inline fun foo(lambda: () -> String = ::prop): String {
    return lambda()
}
