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

open class B() : A(), F {
    {
        order = order + "B"
    }
}

open class A() : F {

    var order = ""
    {
        order = order + "A"
    }
}

trait F {
    fun bar() = "F"
}

fun box() : Boolean {
    return (C().order == "ABC") && (D().order == "ABD") && (E().order == "AE") && (C().bar() == "F") && (A().bar() == "F")
}