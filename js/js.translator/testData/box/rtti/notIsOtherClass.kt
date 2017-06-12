// EXPECTED_REACHABLE_NODES: 500
package foo

open class A() {

}

class B() : A() {

}

fun box(): String {
    assertEquals(true, A() !is B)
    return "OK"
}