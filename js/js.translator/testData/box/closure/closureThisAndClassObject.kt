// EXPECTED_REACHABLE_NODES: 1001
package foo

class A {
    fun foo() = "O"
    companion object {
        fun bar() = "K"
    }

    val f = { foo() + bar() }
}

fun box(): String = A().f()
