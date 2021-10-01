import org.jetbrains.kotlin.fir.plugin.C

@C
class Foo {
    class MyNested
}

class Bar

fun test_1(foo: Foo) {
    val foo2: Foo = foo.materialize()
    val nested = Foo.Nested()
}
