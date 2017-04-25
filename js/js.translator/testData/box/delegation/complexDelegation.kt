// EXPECTED_REACHABLE_NODES: 506
package foo

interface C {
    fun f(): String
}

class B(val value: String) : C {
    override fun f() = value
}

val b: Any = B("O")

val x = B("failure1")
val y = B("K")
val z = B("failure2")

fun selector() = 2

class A : C by (b as C)

class D : C by when (selector()) {
    1 -> x
    2 -> y
    else -> z
}

fun box(): String {
    return A().f() + D().f()
}