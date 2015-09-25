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

fun box(): Boolean {
    return (C().order == "ABC") && (D().order == "ABD") && (E().order == "AE") && (C().bar() == "F") && (A().bar() == "F")
}