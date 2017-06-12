// EXPECTED_REACHABLE_NODES: 491
package foo

class A() {

}

class B() {

}

fun box(): String {
    var c: Int = 0
    var a: Any? = A()
    var b: Any? = null
    when(a) {
        null -> c = 10;
        is B -> c = 10000
        is A -> c = 20;
        else -> c = 1000
    }
    when(b) {
        null -> c += 5
        is B -> c += 100
        else -> c = 1000
    }

    if (c != 25) return "fail: $c"

    return "OK"
}