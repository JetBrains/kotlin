// EXPECTED_REACHABLE_NODES: 490
package foo

var a = 0

class A() {
    init {
        a++
    }

}

fun box(): String {
    var c = A()
    c = A()
    c = A()
    return if (a == 3) "OK" else "fail: $a"
}