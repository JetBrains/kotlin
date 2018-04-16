// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1121
package foo

open class A() {

}

class B() : A() {

}

fun box(): String {
    assertEquals(true, A() !is B)
    return "OK"
}