// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
package foo

class A() {

}

fun box(): String {
    assertEquals(true, A() is A)
    return "OK"
}