
import org.jetbrains.kotlin.plugin.sandbox.NestedClassAndMaterializeMember

@NestedClassAndMaterializeMember
class Foo {
    class MyNested

    val result = "OK"
}

class Bar

fun test(foo: Foo): String {
    val foo2: Foo = foo.materialize()
    val nested = Foo.Nested()
    return foo2.result
}

fun box(): String {
    return test(Foo())
}
