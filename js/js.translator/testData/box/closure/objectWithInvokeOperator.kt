// EXPECTED_REACHABLE_NODES: 536
package foo

object O {
    val result = "O"
}

operator fun O.invoke() = result


class A(val x: Int) {
    companion object {
        val result = "A"
    }
}

operator fun A.Companion.invoke() = result


enum class B {
    E {
        val result = "B"

        override operator fun invoke() = result
    };

    abstract operator fun invoke(): String
}

fun f() = { O() + A() + B.E() }

fun box(): String {
    val result = f()()
    if (result != "OAB") return "expected 'OAB', got '$result'"

    return "OK"
}