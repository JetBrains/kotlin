interface Algebra<T> {
    operator fun T.plus(other: T): T
}

interface A
interface B

fun <T> injectAlgebra() {}

fun test_1(a1: A, a2: A, b1: B, b2: B) {
    a1 <!UNRESOLVED_REFERENCE!>+<!> a2 // error
    b1 <!UNRESOLVED_REFERENCE!>+<!> b2 // error

    injectAlgebra<A>()
    a1 + a2 // ok
    b1 <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> b2 // error

    injectAlgebra<B>()
    a1 + a2 // ok
    b1 + b2 // ok
}

fun test_2(a1: A, a2: A, cond: Boolean) {
    a1 <!UNRESOLVED_REFERENCE!>+<!> a2 // error

    if (cond) {
        injectAlgebra<A>()
        a1 + a2 // ok
    }

    a1 <!UNRESOLVED_REFERENCE!>+<!> a2 // error
}
