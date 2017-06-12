// EXPECTED_REACHABLE_NODES: 497
// This test was adapted from compiler/testData/codegen/box/classes
package foo

interface A<T> {
    fun foo(t: T): String
}

class Derived(a: A<Int>) : A<Int> by a

fun box(): String {
    val o = object : A<Int> {
        override fun foo(t: Int) = if (t == 42) "OK" else "Fail $t"
    }
    return Derived(o).foo(42)
}
