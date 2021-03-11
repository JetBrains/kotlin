// "Remove parameter 'm'" "true"
// DISABLE-ERRORS
fun test() {
    Foo.foo()
    Foo.foo(1<caret>)
    Foo.foo(1, 2)
    Foo.foo(3, 4)
}