// EXPECTED_REACHABLE_NODES: 509
package foo

open class A() {

    var order = ""
    init {
        order = order + "A"
    }
}

open class B() : A() {
    init {
        order = order + "B"
    }
}

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

fun box(): String {
    return if ((C().order == "ABC") && (D().order == "ABD") && (E().order == "AE")) "OK" else "fail"
}