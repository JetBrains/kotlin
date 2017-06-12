// EXPECTED_REACHABLE_NODES: 524
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

open class B() : A() {
    init {
        order = order + "B"
    }
}

open class A() : G() {

    var order = ""
    init {
        order = order + "A"
    }
}

// KT-3437
abstract class G : H()
abstract class K : H()
abstract class L
abstract class H : L()
abstract class Dummy

fun box(): String {
    return if ((C().order == "ABC") && (D().order == "ABD") && (E().order == "AE")) "OK" else "fail"
}