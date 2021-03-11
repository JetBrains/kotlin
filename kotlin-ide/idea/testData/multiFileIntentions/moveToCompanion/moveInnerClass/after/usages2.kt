package test2

import test.A
import test.A.Companion.B

fun foo2(): B {
    return B(A())
}