class Foo(val name: String): Throwable(name) {
    fun bar(): Foo = Foo(name + name)
    val baz = 42
    val qux: String get() = name
}

enum class Bar {
    A, B, C
}

fun foo(times: Int, name: String): List<Foo> = (1..times).map{Foo(name)}
