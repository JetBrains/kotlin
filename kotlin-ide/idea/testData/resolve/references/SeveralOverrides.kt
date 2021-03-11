interface A {
    fun foo()
}

interface B {
    fun foo()
}

interface C : A, B {

}

fun test(c: C) {
    c.<caret>foo()
}

// MULTIRESOLVE
// REF: (in A).foo()
// REF: (in B).foo()