inline fun foo(): String {
    val callableReference: () -> String = ::bar
    return (callableReference().toInt() + 1).toString()
}
