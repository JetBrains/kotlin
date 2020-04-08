// WITH_RUNTIME
// HIGHLIGHT: INFORMATION
fun maybeFoo(): String? {
    return "foo"
}

fun convert(x: String, y: Int) = ""

fun foo(it: Int) {
    val foo = maybeFoo()
    <caret>if (foo == null) else convert(foo, it)
}

