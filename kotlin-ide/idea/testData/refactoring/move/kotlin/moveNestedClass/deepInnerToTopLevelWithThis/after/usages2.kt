package test2

import test.A
import test.C

fun foo2(): C {
    return C(A().B())
}