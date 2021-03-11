// "Add parameter to function 'baz'" "true"
fun baz() {}

fun foo() {
    baz(fun(i: Int): String { return i.toString() }<caret>)
}