package foo

open class A() {

    var order = ""
    {
        order = order + "A"
    }
}

open class B() : A() {
    {
        order = order + "B"
    }
}

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

fun box(): Boolean {
    return (C().order == "ABC") && (D().order == "ABD") && (E().order == "AE")
}