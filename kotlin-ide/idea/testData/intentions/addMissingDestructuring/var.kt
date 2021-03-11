data class Foo(val a: String, val b: String, val c: String)

fun bar(f: Foo) {
    var (a, b<caret>) = f
}
