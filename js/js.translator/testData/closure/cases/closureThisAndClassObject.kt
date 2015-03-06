package foo

class A {
    fun foo() = "O"
    default object {
        fun bar() = "K"
    }

    val f = { foo() + bar() }
}

fun box(): String = A().f()
