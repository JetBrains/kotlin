// EXPECTED_REACHABLE_NODES: 503
package foo

var i = 0

abstract class Base {
    val base = "b" + i++
    inner class Inner {
        fun o() = "O" + base
        fun k() = "K" + base
    }
}

class Child : Base()

fun box(): String {
    var result = ""
    result += Child().Inner().o()

    fun Child.f() {
        result += Inner().k()
    }
    Child().f()

    return if (result == "Ob0Kb1") "OK" else result
}