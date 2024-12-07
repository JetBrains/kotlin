private val prop = "1"

internal inline fun foo(lambda: () -> String = ::prop): String {
    return lambda()
}
