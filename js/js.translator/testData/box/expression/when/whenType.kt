// EXPECTED_REACHABLE_NODES: 1251
package foo

class A() {

}

fun box(): String {
    when(A()) {
        is A -> return "OK"
        else -> return "fail"
    }
}