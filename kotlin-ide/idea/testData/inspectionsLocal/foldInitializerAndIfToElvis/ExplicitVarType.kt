// WITH_RUNTIME

fun foo(): String? = null

fun bar() {
    var v: String? = foo()
    <caret>if (v == null) throw Exception()
    v = null
}