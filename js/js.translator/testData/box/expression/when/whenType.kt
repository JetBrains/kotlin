// EXPECTED_REACHABLE_NODES: 1282
package foo

class A() {

}

fun box(): String {
    when(A()) {
        is A -> return "OK"
        else -> return "fail"
    }
}