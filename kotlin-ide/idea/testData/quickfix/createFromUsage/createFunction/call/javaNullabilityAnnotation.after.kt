// "Create function 'bar'" "true"
// ERROR: Unresolved reference: bar

fun foo(foo: Foo) {
    val s = foo.eval()
    bar(s)
}

fun bar(s: String?) {
    <selection>TODO("Not yet implemented")<caret></selection>
}
