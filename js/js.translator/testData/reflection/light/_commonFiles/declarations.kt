package foo

open class A

class B : A() {
    val a = 1
}

object O

interface I

enum class E {
    X,
    Y {
        val a = 1
    },
    Z {}
}

