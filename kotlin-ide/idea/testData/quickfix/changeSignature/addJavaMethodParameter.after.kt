// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS
fun test() {
    Foo.foo()
    Foo.foo(1)
    Foo.foo(1, 2, 3)
    Foo.foo(1, 2, 3<caret>)
    Foo.foo(4, 5, 6)
}