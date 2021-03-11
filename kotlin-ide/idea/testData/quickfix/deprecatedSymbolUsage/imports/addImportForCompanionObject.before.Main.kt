// "Replace with 'A.bar(x)'" "true"

package test

@Deprecated("bla", ReplaceWith("A.bar(x)", "a.A"))
fun foo(x: Any) {
}

fun test() {
    <caret>foo(1)
}