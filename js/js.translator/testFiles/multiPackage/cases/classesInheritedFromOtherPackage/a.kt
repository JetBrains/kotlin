package foo

import bar.*

open class A() {
    open fun f() = 3;
}

open class C() : B() {
    override fun f() = 5
}

fun box() = (A().f() == 3) && (B().f() == 4) && (C().f() == 5)