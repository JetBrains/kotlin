private inline fun prop() = "6"

internal inline fun foo(lambda: () -> String = ::prop): String {
    return lambda()
}
