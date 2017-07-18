// EXPECTED_REACHABLE_NODES: 999
package foo

open class A() {
    var a = 3;
}

class B() : A() {

}

fun box(): String {
    val a = B().a
    return if (a == 3) "OK" else "Fail, a = $a"
}
