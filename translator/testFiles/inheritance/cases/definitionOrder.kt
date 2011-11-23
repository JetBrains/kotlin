namespace foo

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

open class A() {

    var order = ""
    {
        order = order + "A"
    }
}

fun box() : Boolean {
    return (C().order == "ABC") && (D().order == "ABD") && (E().order == "AE")
}