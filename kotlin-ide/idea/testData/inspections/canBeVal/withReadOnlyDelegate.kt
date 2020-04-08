// WITH_RUNTIME

fun foo() {
    var s: String by lazy { "Hello!" }
    s.hashCode()
}
