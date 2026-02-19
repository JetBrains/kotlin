package app

import lib.*

fun useI(i: I) {
    i.iProperty
    i.iMethod()
}

fun useK(k: A) {
    k.kMethod()
}

fun useA(a: A) {
    a.iProperty
    a.iMethod()

    a.aProperty
    a.aMethod()
    a.aInlineMethod()
    A.aConst
}

fun useB(b: B) {
    b.iProperty
    b.iMethod()

    b.aProperty
    b.aMethod()
    b.aInlineMethod()

    b.bProperty
    b.bMethod()
    b.bInlineMethod()
    B.bConst
}

fun useC(b: String) {
    C(b)
}

fun runAppAndReturnOk(): String {
    useI(A())
    useK(A())
    useA(A())
    useB(B())
    useC("test")

    return "OK"
}