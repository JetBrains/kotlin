// WITH_RUNTIME
fun foo(s: String?) {
    val t = s?.hashCode() ?:<caret> throw Exception()
}
