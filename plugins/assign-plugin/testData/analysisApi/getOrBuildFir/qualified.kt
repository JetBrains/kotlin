annotation class ValueContainer

@ValueContainer
class Container(private var storage: String) {
    fun assign(value: String) {
        storage = value
    }
}

class Foo {
    val property = Container("foo")
}

fun test(foo: Foo) {
    <expr>foo.property</expr> = "bar"
}
