// EXPECTED_REACHABLE_NODES: 510
package foo


open class A() {
    val value = "O"
}

interface Test {
    fun addFoo(s: String): String {
        return s + "K"
    }
}


class B() : Test, A() {
    fun eval(): String {
        return addFoo(value);
    }
}

fun box() = B().eval()