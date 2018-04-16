// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

object Host {
    var result: String = ""
        get
        private set

    operator fun plusAssign(s: String) {
        result += s
    }
}

fun bar() = Host

fun box(): String {
    bar() += "O"
    bar() += "K"
    return Host.result
}