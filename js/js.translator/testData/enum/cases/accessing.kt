package foo

enum class A {
    FOO

    BAR : A() {
        fun explicitFromEntry() = A.FOO
        //fun byThisFromEntry() = this.FOO
        fun implicitFromEntry() = FOO
    }

    fun explicit() = A.FOO
    fun byThis() = this.FOO
    fun implicit() = FOO
}

fun A.extExplicit() = A.FOO
fun A.extByThis() = this.FOO
//fun A.extImplicit() = FOO

fun box(): String {
    assertEquals(A.FOO, A.FOO.explicit(), "explicit access")
    assertEquals(A.FOO, A.FOO.byThis(), "access by this")
    assertEquals(A.FOO, A.FOO.implicit(), "implicit access")

    assertEquals(A.FOO, A.FOO.explicit(), "explicit access from BAR")
    // TODO uncoment when KT-4692 will be fixed
    //assertEquals(A.FOO, A.FOO.byThis(), "access by this from BAR")
    assertEquals(A.FOO, A.FOO.implicit(), "implicit access from BAR")

    assertEquals(A.FOO, A.FOO.extExplicit(), "explicit access from ext fun")
    assertEquals(A.FOO, A.FOO.extByThis(), "access by this from ext fun")
    // TODO uncoment when KT-4692 will be fixed
    //assertEquals(A.FOO, A.FOO.extImplicit(), "implicit access from ext fun")

    return "OK"
}
