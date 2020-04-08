// WITH_RUNTIME
fun test(a: String?, b: String): String {
    val x = if (true) a else b
    <caret>if (x == null) throw Exception()
    return x
}