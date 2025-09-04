inline fun foo(callableReference: () -> String = ::bar): String {
    return callableReference()
}
