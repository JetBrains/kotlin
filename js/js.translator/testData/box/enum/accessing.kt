// EXPECTED_REACHABLE_NODES: 538
package foo

enum class A {
    FOO,

    BAR() {
        fun explicitFromEntry() = A.FOO
        fun implicitFromEntry() = FOO
    };

    fun explicit() = A.FOO
    fun implicit() = FOO
}

fun A.extExplicit() = A.FOO
//fun A.extImplicit() = FOO

fun box(): String {
    assertEquals(A.FOO, A.FOO.explicit(), "explicit access")
    assertEquals(A.FOO, A.FOO.implicit(), "implicit access")

    assertEquals(A.FOO, A.FOO.explicit(), "explicit access from BAR")
    assertEquals(A.FOO, A.FOO.implicit(), "implicit access from BAR")

    assertEquals(A.FOO, A.FOO.extExplicit(), "explicit access from ext fun")
    // TODO uncoment when KT-5605 will be fixed
    //assertEquals(A.FOO, A.FOO.extImplicit(), "implicit access from ext fun")

    return "OK"
}
