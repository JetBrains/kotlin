// KT-2995 creating factory methods to simulate overloaded constructors don't work in JavaScript

package foo

class Foo(val name: String)

fun Foo() = Foo("<default-name>")

fun box(): String {
    assertEquals("<default-name>", Foo().name)
    assertEquals("BarBaz", Foo("BarBaz").name)

    return "OK"
}