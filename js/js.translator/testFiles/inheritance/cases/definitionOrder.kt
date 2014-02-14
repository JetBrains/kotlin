package foo

class C() : B() {
    {
        order = order + "C"
    }
}

class D() : B() {
    {
        order = order + "D"
    }
}

class E() : A() {
    {
        order = order + "E"
    }
}

open class B() : A() {
    {
        order = order + "B"
    }
}

open class A() : G() {

    var order = ""
    {
        order = order + "A"
    }
}

// KT-3437
abstract class G : H()
abstract class K : H()
abstract class L
abstract class H : L()
abstract class Dummy

fun box(): Boolean {
    return (C().order == "ABC") && (D().order == "ABD") && (E().order == "AE")
}