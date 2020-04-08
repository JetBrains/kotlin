// IS_APPLICABLE: false
interface Foo {
    operator fun get(x : Any) : Foo
    operator fun plus(x : Any) : Foo
}
fun foo(x: Foo) {
    <caret>(x + x)[x]
}