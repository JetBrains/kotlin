// EXPECTED_REACHABLE_NODES: 1253
package foo

class A() {

}

fun box(): String {
    assertEquals(true, A() is A)
    return "OK"
}