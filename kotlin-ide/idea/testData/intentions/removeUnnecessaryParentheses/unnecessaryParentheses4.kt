interface Foo {
    operator fun inc() : Foo
    operator fun not() : Foo
}
fun foo(x: Foo) {
    !(<caret>x.inc())
}