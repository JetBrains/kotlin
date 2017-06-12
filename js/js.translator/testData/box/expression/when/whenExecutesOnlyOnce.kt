// EXPECTED_REACHABLE_NODES: 489
package foo

class A() {

}

fun box(): String {
    var a = 0
    when(A()) {
        is A -> a++;
        is A -> a++;
        else -> a++;
    }
    if (a != 1) return "fail: $a"
    return "OK"
}