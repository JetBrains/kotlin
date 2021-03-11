// WITH_RUNTIME
fun test(a: String, b: String): Boolean {
    return <caret>a.toUpperCase() == b.toUpperCase()
}