// EXPECTED_REACHABLE_NODES: 492
package foo

var i = 0

class A() {
    override fun toString(): String {
        i++
        return "bar"
    }
}

fun box(): String {
    val a = A()
    val s = "$a == $a"
    return if (s == "bar == bar" && i == 2) "OK" else "fail"
}