import org.jetbrains.kotlin.fir.plugin.NestedClassAndMaterializeMember

@NestedClassAndMaterializeMember
class Foo {
    class MyNested
}

class Bar

fun test_1(foo: Foo) {
    val foo2: Foo = foo.materialize()
    val nested = Foo.Nested()
}

// should be errors
fun test_2(bar: Bar) {
    val foo2: Bar = bar.<!UNRESOLVED_REFERENCE!>materialize<!>()
    val nested = Bar.<!UNRESOLVED_REFERENCE!>Nested<!>()
}
