// EXPECTED_REACHABLE_NODES: 493
package foo

class A() {

}

fun box(): String {
    assertEquals(true, A() is A)
    return "OK"
}