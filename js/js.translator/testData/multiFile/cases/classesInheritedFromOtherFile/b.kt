package foo

open class B() : A() {
    override fun f() = 4
}

fun box() = (A().f() == 3) && (B().f() == 4) && (C().f() == 5)