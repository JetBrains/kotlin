private val prop = "0"

internal inline fun foo(lambda: () -> String = ::prop): String {
    return lambda()
}
