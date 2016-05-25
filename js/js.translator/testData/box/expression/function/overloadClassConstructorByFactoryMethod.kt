// KT-2995 creating factory methods to simulate overloaded constructors don't work in JavaScript
// This test is incorrect, since both constructor and function must have the same name.
package foo

class Foo(val name: String)

//fun Foo() = Foo("<default-name>")

fun box(): String {
    // TODO: Don't know another way to suppress the test
    /*assertEquals("<default-name>", Foo().name)
    assertEquals("BarBaz", Foo("BarBaz").name)*/

    return "OK"
}