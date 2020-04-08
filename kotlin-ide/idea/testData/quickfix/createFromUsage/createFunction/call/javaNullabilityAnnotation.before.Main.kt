// "Create function 'bar'" "true"
// ERROR: Unresolved reference: bar

fun foo(foo: Foo) {
    val s = foo.eval()
    <caret>bar(s)
}