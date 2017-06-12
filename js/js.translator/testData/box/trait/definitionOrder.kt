// EXPECTED_REACHABLE_NODES: 520
package foo

class C() : B() {
    init {
        order = order + "C"
    }
}

class D() : B() {
    init {
        order = order + "D"
    }
}

class E() : A() {
    init {
        order = order + "E"
    }
}

open class B() : A(), F {
    init {
        order = order + "B"
    }
}

open class A() : F {

    var order = ""
    init {
        order = order + "A"
    }
}

interface F : G {
    fun bar() = "F"
}

// KT-3437
interface G : H
interface K : H
interface L
interface H : L
interface Dummy

fun box(): String {
    if (C().order != "ABC") return "fail1"
    if (D().order != "ABD") return "fail2"
    if (E().order != "AE") return "fail3"
    if (C().bar() != "F") return "fail4"
    if (A().bar() != "F") return "fail5"

    return "OK"
}