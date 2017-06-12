// EXPECTED_REACHABLE_NODES: 489
package foo

class A() {

}

fun box(): String {
    when(A()) {
        !is A -> return "fail"
        else -> return "OK"
    }
}