package testing.rename

public open class Foo(public open val /*rename*/first: String)

public class Bar : Foo("abc") {
    override val first = "xyzzy"
}

fun usages(f: Foo, b: Bar): String {
    return f.first + b.first
}
