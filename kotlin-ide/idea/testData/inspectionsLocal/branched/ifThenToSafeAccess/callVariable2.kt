class Foo(val f: () -> Unit)

fun test(foo: Foo?) {
    <caret>if (foo != null) {
        foo.f()
    }
}