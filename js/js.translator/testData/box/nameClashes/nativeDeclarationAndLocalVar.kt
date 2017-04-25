// EXPECTED_REACHABLE_NODES: 489
package test

external fun foo(): dynamic

external fun bar(): dynamic

fun box(): String {
    val foo = "local foo;"
    val bar = "local bar;"
    val result = foo + test.bar() + test.foo() + bar
    if (result != "local foo;global bar;global foo;local bar;") return "fail: $result"

    return "OK"
}