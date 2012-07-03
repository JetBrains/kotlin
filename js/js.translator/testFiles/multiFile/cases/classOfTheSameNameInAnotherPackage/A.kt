package foo

open class A() {
    fun f() = 3
}

fun box() = (A().f() + bar.A().f()) == 9