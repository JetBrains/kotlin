// ERROR: 'public' function exposes its 'internal' parameter type Foo
internal interface Foo
class Bar {
    fun test() {
        object : Foo {
            fun foo() {
                bug(this)
            }
        }
    }

    fun bug(foo: Foo?) {}
}
