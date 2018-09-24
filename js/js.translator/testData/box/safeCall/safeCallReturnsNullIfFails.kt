// EXPECTED_REACHABLE_NODES: 1280
package foo

class A() {
    val x = 4
}

fun box(): String {
    var a: A? = null;
    return if (a?.x == null) "OK" else "fail"
}