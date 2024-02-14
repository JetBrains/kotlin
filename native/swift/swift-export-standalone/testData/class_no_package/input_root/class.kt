class Foo {
    // we do not handle inner declarations of class currently. This fun will not be exported
    fun foo()
}
fun bar(p: Foo): Foo = TODO()

var foo: Foo
