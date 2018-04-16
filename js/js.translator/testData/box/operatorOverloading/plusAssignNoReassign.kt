// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1112
package foo

class A() {

    var message = ""
    operator fun plusAssign(other: A) {
        message = message + "!"
    }

}

fun box(): String {
    var c = A()
    c += A()
    c += A()
    return if (c.message == "!!") return "OK" else "fail"
}