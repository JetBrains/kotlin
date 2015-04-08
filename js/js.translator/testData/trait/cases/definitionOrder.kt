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

trait F : G {
    fun bar() = "F"
}

// KT-3437
trait G : H
trait K : H
trait L
trait H : L
trait Dummy

fun box(): Boolean {
    return (C().order == "ABC") && (D().order == "ABD") && (E().order == "AE") && (C().bar() == "F") && (A().bar() == "F")
}