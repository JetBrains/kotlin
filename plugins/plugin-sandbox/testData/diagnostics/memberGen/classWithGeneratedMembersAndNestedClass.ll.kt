// LL_FIR_DIVERGENCE
// KT-75132
// LL_FIR_DIVERGENCE
import org.jetbrains.kotlin.plugin.sandbox.NestedClassAndMaterializeMember

@NestedClassAndMaterializeMember
class Foo {
    class MyNested
}

class Bar

fun test_1(foo: Foo) {
    val foo2: Foo = foo.<!UNRESOLVED_REFERENCE!>materialize<!>()
    val nested = Foo.<!UNRESOLVED_REFERENCE!>Nested<!>()
}

// should be errors
fun test_2(bar: Bar) {
    val foo2: Bar = bar.<!UNRESOLVED_REFERENCE!>materialize<!>()
    val nested = Bar.<!UNRESOLVED_REFERENCE!>Nested<!>()
}
