// EXPECTED_REACHABLE_NODES: 494
// KT-2995 creating factory methods to simulate overloaded constructors don't work in JavaScript
package foo

class Foo(val name: String)

fun Foo(x: Int) = Foo("<$x>")

fun box(): String {
    assertEquals("<123>", Foo(123).name)
    assertEquals("BarBaz", Foo("BarBaz").name)

    return "OK"
}