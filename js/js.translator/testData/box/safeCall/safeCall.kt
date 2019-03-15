// EXPECTED_REACHABLE_NODES: 1280
package foo

class A() {
    fun doSomething() {
    }
}

fun box(): String {
    var a: A? = null;
    a?.doSomething()
    return "OK"
}