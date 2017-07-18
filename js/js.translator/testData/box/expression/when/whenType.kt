// EXPECTED_REACHABLE_NODES: 992
package foo

class A() {

}

fun box(): String {
    when(A()) {
        is A -> return "OK"
        else -> return "fail"
    }
}